// Public browser destinations only. Deck content is imported through the engine.
function commanderBrowse(sortColumn = 'updated') {
  if (!['updated', 'views'].includes(sortColumn)) throw new Error('Unknown deck sort');
  const query = { hub: '', format: 'commander', deckName: '', cardId: '', cardName: '', board: '', lastSearch: '',
    filter: '', authorUserNames: '', commanderCardId: '', commanderCardName: '', partnerCardId: '', partnerCardName: '',
    commanderSignatureSpellCardId: '', commanderSignatureSpellCardName: '', partnerSignatureSpellCardId: '',
    partnerSignatureSpellCardName: '', companionCardId: '', companionCardName: '', bracketSetting: 'equals', bracket: '',
    sortColumn, sortDirection: 'descending', pageNumber: 1, pageSize: 64, view: 'public', hubName: '' };
  return 'https://moxfield.com/decks/public?q=' + encodeURIComponent(Buffer.from(JSON.stringify(query)).toString('base64'));
}
function publicDeckUrl(value) {
  const url = new URL(value);
  if (url.protocol !== 'https:' || url.hostname !== 'moxfield.com' || url.port || url.username || url.password
    || !/^\/decks\/[\w-]{22}$/.test(url.pathname) || url.search || url.hash) throw new Error('Unknown deck source');
  return url.href;
}
module.exports = { commanderBrowse, publicDeckUrl };
