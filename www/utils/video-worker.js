self.addEventListener("message", async function (e) {
  if (e.origin && e.origin !== self.origin) return;

  const { snapshotCanvas, width, height, quality } = e.data;

  const context = snapshotCanvas.getContext("2d");
  context.drawImage(snapshotCanvas, 0, 0, width, height);

  const blob = await snapshotCanvas.convertToBlob({ type: "image/jpeg", quality });
  const reader = new FileReader();

  reader.onload = function (event) {
    const dataURI = event.target.result;
    self.postMessage(dataURI);
  };

  reader.readAsDataURL(blob);
});
