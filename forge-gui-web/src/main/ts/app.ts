// The page's text is fetched from the server first. The modules that draw the page load only after it arrives

import { loadText } from './text';

await loadText();
await import('./main');
