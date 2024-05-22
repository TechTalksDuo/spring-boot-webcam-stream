export function getRandomColor() {
  return (
    "#" +
    Math.floor(Math.random() * 2 ** 24)
      .toString(16)
      .padStart(6, "0")
  );
}
