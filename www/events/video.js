class VideoEventTarget extends EventTarget {
  events = {
    start: "start",
    stop: "stop",
  };

  start() {
    this.dispatchEvent(new CustomEvent(VideoEventTarget.events.start));
  }

  stop() {
    this.dispatchEvent(new CustomEvent(VideoEventTarget.events.stop));
  }
}

export const VideoState = new VideoEventTarget();
