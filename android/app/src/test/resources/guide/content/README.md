# Content-engine test fixtures (v2.3.4, workstream B)

All fixtures here are **SYNTHETIC**. They were written by hand in the exact shape of the live
MediaWiki Action API responses (formatversion=2), because the build sandbox had no network access
to record real ones.

- QIDs `Q90001xx`–`Q90005xx` and revision ids `4400000`/`45001xx` are placeholders, not real
  Wikidata or Wikivoyage ids. Well-known class/country ids (Q668 India, Q837 Nepal, Q515 city,
  Q532 village, Q3957 town, Q9259 World Heritage Site, Q4167410 disambiguation page) are real.
- `*.wikitext` files hold article bodies written in Wikivoyage style. The tests wrap them in an
  `action=parse` envelope (see `Fixtures.parseResponse`).
- `wikidata_ambiguous_aurangabad.json` and `wikidata_gokarna.json` combine a search payload and an
  entities payload under `search_response` and `entities_response`.

To replace a fixture with a recorded one, fetch it with `User-Agent: GuideHttp.USER_AGENT`, keep the
file name, and adjust the ids asserted in the tests.
