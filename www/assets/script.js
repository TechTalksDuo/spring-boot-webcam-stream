globalThis.socket = null;
globalThis.users = {};

const videoPreview = document.querySelector("img#preview");
const video = document.querySelector("video#mirror");
const canvas = document.querySelector("#paint");
let { socket } = globalThis;
let localVideoStream;
let videoUpdateInterval;

function randomColor() {
  return (
    "#" +
    Math.floor(Math.random() * 2 ** 24)
      .toString(16)
      .padStart(6, "0")
  );
}

function dataURItoBlob(dataURI) {
  // convert base64 to raw binary data held in a string
  // doesn't handle URLEncoded DataURIs - see SO answer #6850276 for code that does this
  const byteString = atob(dataURI.split(",")[1]);

  // separate out the mime component
  const mimeString = dataURI.split(",")[0].split(":")[1].split(";")[0];

  // write the bytes of the string to an ArrayBuffer
  const ab = new ArrayBuffer(byteString.length);

  // create a view into the buffer
  const ia = new Uint8Array(ab);

  // set the bytes of the buffer to the correct values
  for (let i = 0; i < byteString.length; i++) {
    ia[i] = byteString.charCodeAt(i);
  }

  // write the ArrayBuffer to a blob, and you're done
  return new Blob([ab], { type: mimeString });
}

function connect() {
  socket = new WebSocket("wss://yolo.go.ro/websocket");
  console.log("Connecting...");
  socket.onopen = () => console.log("Connected");
  socket.onerror = (error) => console.error(`SocketError: ${error.message}`);
  socket.onmessage = (message) => {
    readMessage(JSON.parse(message.data));
  };
}

video.style.setProperty("--color", randomColor());
computeTileSize();

function disconnect() {
  console.log("Disconnecting...");
  socket.onclose = () => console.log("Disconnected");
  socket?.close();
}

function sendMessage(message) {
  socket.send(JSON.stringify(message));
}

async function readMessage(message) {
  const { updates, username, videoStream } = message;

  users[username] = { ...users[username], updates: updates ?? [{ x: 0, y: 0 }], videoStream };

  if (videoStream) {
    URL.revokeObjectURL(videoPreview.src);
    videoPreview.src = URL.createObjectURL(dataURItoBlob(videoStream));
  }
}

async function startVideoStream() {
  localVideoStream = await navigator.mediaDevices.getUserMedia({
    audio: false,
    video: {
      width: { min: 128, ideal: 256 },
      height: { min: 128, ideal: 256 },
      frameRate: { ideal: 24, min: 12 },
      facingMode: "user",
    },
  });

  video.srcObject = localVideoStream;

  const snapshotCanvas = document.createElement("canvas");
  snapshotCanvas.width = localVideoStream.getVideoTracks()[0].getSettings().width;
  snapshotCanvas.height = localVideoStream.getVideoTracks()[0].getSettings().height;

  // videoUpdateInterval = setInterval(() => {
  //   snapshotCanvas.getContext("2d").clearRect(0, 0, snapshotCanvas.width, snapshotCanvas.height);
  //   snapshotCanvas.getContext("2d").drawImage(video, 0, 0);
  //   const encodedData = snapshotCanvas.toDataURL("image/jpeg", 0.5);
  //   socket?.send(JSON.stringify({ videoStream: encodedData.split(",")[1] }));
  // }, 1000 / 1);
}

async function stopVideoStream() {
  if (localVideoStream) localVideoStream.getTracks().forEach((track) => track.stop());
  clearInterval(videoUpdateInterval);
  const video = document.querySelector("video#mirror");
  video.srcObject = null;
}

function computeTileSize() {
  const grid = document.querySelector("main");
  const items = grid.querySelectorAll(".preview");
  const optimumColumns = Math.ceil(Math.sqrt(items.length));
  const optimumRows = Math.ceil(items.length / optimumColumns);
  const optimumGridSize = optimumColumns * optimumRows;

  grid.style.gridTemplateColumns = `repeat(${optimumColumns}, ${Math.ceil(
    grid.clientWidth / optimumColumns
  )}px)`;
  grid.style.gridTemplateRows = `repeat(${optimumRows}, ${Math.ceil(
    grid.clientHeight / optimumRows
  )}px)`;

  if (grid.clientHeight > grid.clientWidth) grid.style.gridAutoFlow = "row";

  items.forEach((item, index) => {
    item.style.width = `${Math.ceil(grid.clientWidth / optimumColumns)}px`;
    item.style.height = `${Math.ceil(grid.clientHeight / optimumRows)}px`;

    if (optimumGridSize > items.length && index === items.length - 1) {
      if (grid.clientWidth > grid.clientHeight) {
        item.style.height = `${Math.ceil(
          (grid.clientHeight / optimumRows) * (1 + optimumGridSize - items.length)
        )}px`;
      } else {
        item.style.width = `${Math.ceil(
          (grid.clientWidth / optimumColumns) * (1 + optimumGridSize - items.length)
        )}px`;
      }
    }
  });
}

window.addEventListener("resize", computeTileSize);
window.screen.orientation.addEventListener("change", computeTileSize);
