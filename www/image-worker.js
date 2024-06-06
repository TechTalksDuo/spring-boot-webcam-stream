self.addEventListener(
  "message",
  async function (e) {
    if (e.origin !== self.origin) return;

    const dataURI = e.data;

    if (!dataURI) return null;
    const byteString = atob(dataURI.split(",")[1]);
    const mimeString = dataURI.split(",")[0].split(":")[1].split(";")[0];
    const arrayBuffer = new ArrayBuffer(byteString.length);
    const intArray = new Uint8Array(arrayBuffer);

    for (let i = 0; i < byteString.length; i++) {
      intArray[i] = byteString.charCodeAt(i);
    }

    const blob = new Blob([arrayBuffer], { type: mimeString });
    const image = URL.createObjectURL(blob);

    self.postMessage(image);
  },
  false
);
