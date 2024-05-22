import { LitElement, html, css } from "https://esm.run/lit/index.js";

import { WebSocketState, WebSocketEvent, WebSocketEventType } from "../events/websocket.js";
import { UserState } from "../events/user.js";

class ConnectionControls extends LitElement {
  constructor() {
    super();

    this.userCount = 0;

    WebSocketState.addEventListener(WebSocketEvent.open, () => {
      this.isConnected = true;
    });

    WebSocketState.addEventListener(WebSocketEvent.message, ({ detail }) => {
      switch (detail.type) {
        case WebSocketEventType.userConnected:
          UserState.me = detail.me;
          UserState.activeUsers = detail?.onlineUsers?.length;
          detail?.onlineUsers.forEach(({ username }) => UserState.join(username));
          break;
        case WebSocketEventType.userJoined:
          UserState.join(detail.username);
          UserState.activeUsers = detail?.activeUsers;
          break;
        case WebSocketEventType.userLeft:
          UserState.leave(detail.username);
          UserState.activeUsers = detail?.activeUsers;
          break;
      }

      this.userCount = UserState.activeUsers;
    });

    WebSocketState.addEventListener(WebSocketEvent.close, () => {
      this.userCount = 0;
      this.isConnected = false;
      UserState.clear();
    });
  }

  static properties = {
    userCount: { type: Number },
    isConnected: { type: Boolean },
  };

  static styles = css`
    :host {
      padding: 1rem;
      text-align: center;

      display: flex;
      align-items: center;
      justify-content: space-between;

      font-size: 1.5rem;
    }

    button {
      padding: 1rem;
      background-color: #17e;
      border-radius: 0.5rem;
      border: 0.25rem solid;
      color: #fff;
      font-size: inherit;
      display: inline-flex;
      gap: 0.5rem;

      &.leave {
        background-color: #d24;
      }
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

  render() {
    return html`
      <strong>👥 ${this.userCount}</strong>
      <section>
        ${this.isConnected
          ? html`<button class="leave" @click=${() => WebSocketState.close()}>
              <svg class="feather">
                <use href="/assets/feather-sprite.svg#wifi-off" />
              </svg>
              Disconnect
            </button>`
          : html`<button class="join" @click=${() => WebSocketState.open()}>
              <svg class="feather">
                <use href="/assets/feather-sprite.svg#wifi" />
              </svg>
              Connect
            </button>`}
      </section>
    `;
  }
}

customElements.define("connection-controls", ConnectionControls);
