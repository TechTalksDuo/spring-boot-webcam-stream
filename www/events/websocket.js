export class WebSocketEvent {
  static open = "open";
  static close = "close";
  static error = "error";
  static message = "message";
}

export class WebSocketEventType {
  static userConnected = "USER_CONNECTED";
  static userJoined = "USER_JOINED";
  static userLeft = "USER_LEFT";
  static videoFromUser = "VIDEO_FROM_USER";
  static videoStopped = "VIDEO_STOPPED";
  static videoFeedback = "VIDEO_FEEDBACK";
}

class WebSocketEventTarget extends EventTarget {
  #websocket;

  open() {
    if (this.readyState !== 3) return;

    this.#websocket = new WebSocket("wss://yolo.go.ro/websocket");
    console.info("WebSocket opening...");
    this.#websocket.onopen = () => {
      console.info("WebSocket opened");
      this.dispatchEvent(new Event(WebSocketEvent.open));
    };
    this.#websocket.onclose = () => {
      console.info("WebSocket closed");
      this.dispatchEvent(new Event(WebSocketEvent.close));
    };
    this.#websocket.onerror = ({ message, ...meta }) => {
      console.error(`WebSocketError: ${message}`);
      this.dispatchEvent(new CustomEvent(WebSocketEvent.error, { detail: message }));
    };
    this.#websocket.onmessage = ({ data, ...meta }) => {
      try {
        this.dispatchEvent(new CustomEvent(WebSocketEvent.message, { detail: JSON.parse(data) }));
      } catch (error) {
        this.dispatchEvent(new CustomEvent(WebSocketEvent.error, { detail: error.message }));
      }
    };
  }

  close() {
    if (this.readyState > 1) return;

    console.info("WebSocket closing...");
    this.#websocket.close();
  }

  send(message) {
    if (this.readyState !== 1) return;
    this.#websocket.send(message);
  }

  get readyState() {
    return this.#websocket?.readyState ?? 3;
  }
}

export const WebSocketState = new WebSocketEventTarget();
