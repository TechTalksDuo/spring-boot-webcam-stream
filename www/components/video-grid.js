import { LitElement, html, css } from "https://esm.run/lit/index.js";

import "./video-tile.js";
import { getRandomColor } from "../utils/color.js";
import { UserEvents, UserState } from "../events/user.js";
import { WebSocketEvent, WebSocketState } from "../events/websocket.js";

class VideoGrid extends LitElement {
  #localVideoStream;
  #computeTileSizeCallback;
  #videoUpdateInterval;
  #videoQuality = 0.7;

  constructor() {
    super();

    this.connectedUsers = [];
    UserState.addEventListener(UserEvents.update, () => {
      this.connectedUsers = [...UserState.users];
      this.#videoQuality = 0.7 - Math.log10(Math.sqrt(Math.sqrt(this.connectedUsers.length || 1)));
    });
    WebSocketState.addEventListener(WebSocketEvent.close, () => {
      clearInterval(this.#videoUpdateInterval);
    });
  }

  static properties = {
    isVideoActive: { type: Boolean },
    connectedUsers: { type: Array },
  };

  static styles = css`
    :host {
      display: grid;
      overflow: hidden;
      grid-auto-flow: column;
    }

    .user-video {
      display: grid;
      grid-template-areas: "video";

      .controls {
        z-index: 1;
        grid-area: video;
        display: flex;
        place-self: center;

        button {
          padding: 1rem;
          background-color: #17e;
          border-radius: 0.5rem;
          border: 0.25rem solid;
          color: #fff;
          font-size: 1.5rem;
          display: inline-flex;
          gap: 0.5rem;

          &.stop {
            background-color: #d24;
          }
        }
      }

      &:has(.streaming) {
        &:not(:hover) .streaming {
          display: none;
        }
      }

      .username:not(:empty) {
        grid-area: video;
        justify-self: start;
        align-self: end;
        padding: 0.5rem 1rem;
        background-color: #17e;
        color: #fff;
        border-radius: 0 0.5rem 0 0;
      }
    }

    video {
      object-fit: cover;
      background-color: var(--color, transparent);
      grid-area: video;
    }

    .feather {
      width: 1.5rem;
      height: 1.5rem;
      stroke: currentColor;
      stroke-width: 2;
      stroke-linecap: round;
      stroke-linejoin: round;
      fill: none;
    }
  `;

  firstUpdated() {
    const video = this.shadowRoot.querySelector("video");
    this.#computeTileSize();
    video.style.setProperty("--color", getRandomColor());
    this.#computeTileSizeCallback = this.#computeTileSize.bind(this);
    window.addEventListener("resize", this.#computeTileSizeCallback);
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    window.removeEventListener("resize", this.#computeTileSizeCallback);
  }

  render() {
    return html`
      <section class="user-video">
        <video autoplay playsinline muted></video>
        <span class="controls ${this.isVideoActive ? "streaming" : ""}">
          ${this.isVideoActive
            ? html`<button @click=${this.#stopVideo} class="stop">
                <svg class="feather">
                  <use href="/assets/feather-sprite.svg#video-off" />
                </svg>
                Stop
              </button>`
            : html`<button @click=${this.#startVideo}>
                <svg class="feather">
                  <use href="/assets/feather-sprite.svg#video" />
                </svg>
                Stream
              </button>`}
        </span>
        <span class="username">${UserState.me?.username}</span>
      </section>
      ${this.connectedUsers.map(
        ({ username }) => html` <video-tile .username=${username}></video-tile> `
      )}
    `;
  }

  updated() {
    this.#computeTileSize();
  }

  #computeTileSize() {
    const items = this.shadowRoot.querySelectorAll("video, video-tile");
    const optimumColumns = Math.ceil(Math.sqrt(items.length));
    const optimumRows = Math.ceil(items.length / optimumColumns);
    const optimumGridSize = optimumColumns * optimumRows;

    this.style.gridTemplateColumns = `repeat(${optimumColumns}, ${Math.ceil(
      this.clientWidth / optimumColumns
    )}px)`;
    this.style.gridTemplateRows = `repeat(${optimumRows}, ${Math.ceil(
      this.clientHeight / optimumRows
    )}px)`;

    if (this.clientHeight > this.clientWidth) this.style.gridAutoFlow = "row";
    else this.style.gridAutoFlow = "column";

    items.forEach((item, index) => {
      item.style.width = `${Math.ceil(this.clientWidth / optimumColumns)}px`;
      item.style.height = `${Math.ceil(this.clientHeight / optimumRows)}px`;

      if (optimumGridSize > items.length && index === items.length - 1) {
        if (this.clientWidth > this.clientHeight) {
          item.style.height = `${Math.ceil(
            (this.clientHeight / optimumRows) * (1 + optimumGridSize - items.length)
          )}px`;
        } else {
          item.style.width = `${Math.ceil(
            (this.clientWidth / optimumColumns) * (1 + optimumGridSize - items.length)
          )}px`;
        }
      }
    });
  }

  async #startVideo() {
    const video = this.shadowRoot.querySelector("video");
    this.#localVideoStream = await navigator.mediaDevices.getUserMedia({
      audio: false,
      video: {
        width: { min: 128, ideal: 256 },
        height: { min: 128, ideal: 256 },
        frameRate: { ideal: 24, min: 12 },
        facingMode: "user",
      },
    });

    video.srcObject = this.#localVideoStream;
    this.isVideoActive = true;

    this.#streamVideo(video, this.#localVideoStream);
  }

  #stopVideo() {
    const video = this.shadowRoot.querySelector("video");
    this.#localVideoStream.getTracks().forEach((track) => track.stop());
    clearInterval(this.#videoUpdateInterval);
    video.srcObject = null;
    this.isVideoActive = false;
  }

  #streamVideo(video, localVideoStream) {
    const snapshotCanvas = document.createElement("canvas");
    const { width, height } = localVideoStream.getVideoTracks()[0].getSettings();
    snapshotCanvas.width = width;
    snapshotCanvas.height = height;

    this.#videoUpdateInterval = setInterval(() => {
      snapshotCanvas.getContext("2d").clearRect(0, 0, width, height);
      snapshotCanvas.getContext("2d").drawImage(video, 0, 0, width, height);
      const encodedData = snapshotCanvas.toDataURL("image/jpeg", this.#videoQuality);
      WebSocketState?.send(JSON.stringify({ videoStream: encodedData }));
    }, 1000 / 1);
  }
}

customElements.define("video-grid", VideoGrid);
