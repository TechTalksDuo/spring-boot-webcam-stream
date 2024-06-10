import { LitElement, html, css } from "https://esm.run/lit/index.js";

import { WebSocketState, WebSocketEvent, WebSocketEventType } from "../events/websocket.js";

class VideoFeedback extends LitElement {
  #resizeObserver;

  constructor() {
    super();

    WebSocketState.addEventListener(WebSocketEvent.message, ({ detail }) => {
      if (detail.type === WebSocketEventType.videoFeedback && detail.username === this.username) {
        this.#updateEmotion(detail.emotion);
      }
    });
  }

  static properties = {
    username: { type: String },
  };

  static styles = css`
    svg {
      width: 100%;
      height: 100%;

      text {
        animation: scroll 2s ease-in forwards;
      }
    }

    @keyframes scroll {
      100% {
        transform: translateY(-100%);
      }
    }
  `;

  firstUpdated() {
    const svg = this.shadowRoot.querySelector("svg");
    // this.#resizeCanvas(canvas);
    this.#resizeObserver = new ResizeObserver(() => this.#resizeSvg(svg));
    this.#resizeObserver.observe(this);
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this.#resizeObserver.unobserve(this);
  }

  render() {
    return html`<svg xmlns="http://www.w3.org/2000/svg"></svg>`;
  }

  #updateEmotion({ emoji, score = 1.0 }) {
    if (!emoji) return;

    const svg = this.shadowRoot.querySelector("svg");
    const text = document.createElementNS("http://www.w3.org/2000/svg", "text");

    text.textContent = String(emoji);
    text.setAttribute("font-size", score * 64);
    text.setAttribute("x", `${16 + Math.random() * 84}%`);
    text.setAttribute("y", "100%");

    svg.append(text);

    setTimeout(() => text.remove(), 2_000);
  }

  #resizeSvg(svg) {
    const { width, height } = svg.getBoundingClientRect();
    svg.setAttribute("width", width);
    svg.setAttribute("height", height);
  }
}

customElements.define("video-feedback", VideoFeedback);
