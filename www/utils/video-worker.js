self.addEventListener("message", async function (e) {
  if (e.origin && e.origin !== self.origin) return;

  const { video, width, height, quality } = e.data;

  const canvas = new OffscreenCanvas(width, height);
  const context = canvas.getContext("2d");

  context.drawImage(video, 0, 0, width, height);
  const blob = await canvas.convertToBlob({ type: "image/jpeg", quality });
  const reader = new FileReader();
  reader.onload = function (event) {
    const dataURI = event.target.result;
    self.postMessage(dataURI);
  };
  reader.readAsDataURL(blob);
});
