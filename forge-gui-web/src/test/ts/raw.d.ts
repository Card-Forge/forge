// A file imported with ?raw. Vite returns its content as a string
declare module '*?raw' {
  const text: string;
  export default text;
}
