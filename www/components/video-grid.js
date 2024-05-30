import { LitElement, html, css } from "https://esm.run/lit/index.js";

import "./video-source.js";
import "./video-tile.js";
import { UserEvents, UserState } from "../events/user.js";

class VideoGrid extends LitElement {
  #computeTileSizeCallback;

  constructor() {
    super();

    this.connectedUsers = [];
    UserState.addEventListener(UserEvents.update, () => {
      this.connectedUsers = [...UserState.users];
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
  `;

  firstUpdated() {
    this.#computeTileSize();
    this.#computeTileSizeCallback = this.#computeTileSize.bind(this);
    window.addEventListener("resize", this.#computeTileSizeCallback);
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    window.removeEventListener("resize", this.#computeTileSizeCallback);
  }

  render() {
    return html`
      <video-source></video-source>
      ${this.connectedUsers.map(
        ({ username }) => html` <video-tile .username=${username}></video-tile> `
      )}
    `;
  }

  updated() {
    this.#computeTileSize();
  }

  #computeTileSize() {
    const items = this.shadowRoot.querySelectorAll("video-source, video-tile");
    const isWider = this.clientWidth > this.clientHeight;
    let optimumRows = 0,
      optimumColumns = 0;

    if (isWider) {
      optimumColumns = Math.ceil(Math.sqrt(items.length));
      optimumRows = Math.ceil(items.length / optimumColumns);
    } else {
      optimumRows = Math.ceil(Math.sqrt(items.length));
      optimumColumns = Math.ceil(items.length / optimumRows);
    }

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
}

customElements.define("video-grid", VideoGrid);
