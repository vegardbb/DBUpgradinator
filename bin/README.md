# Programmeringskladd
Tanker om programmeringen og modelleringen rundt masteroppgaven

## Stikkord
* Språk: Java
* Type: NoSQL
* Basert på: Voldemort
* Status: Arkitektur av testapplikasjon uferdig

## Klasser som må implementeres:
* `package voldemort.tools` (ekstra)
  * En fin idé hadde vært å implementere et konsollbasert spørregrensesnitt eller et annet Admin-verktøy der man kan operere på det implisitte dataskjemaet ved å definere endringer i et ordnet format, noe KVolve savner i sin proof-of-concept-implementasjon
* `package voldemort.dbupgradinator`
  * AbstractAggregateTransformer - en abstrakt klasse med en tilstandsvariabel som indikerer applikasjonsversjonen oppgraderingsklassen opererer på og en som indikerer nøkkelverdien til den neste applikasjonen som den transformerte verdien gjelder for; klassen har to abstrakte metoder, som implementeres i subklassen programmert og kompilert til `.class`-fil av den individuelle applikasjonsprogrammerer:
    * TransformGetQuery - påkalles når klienten mottar data fra datalageret, etter at en `InconsistencyResolver` har flettet divergerende elementer; argumenter: key (fra DB), value (deserialisert); returnerer: 
    * TransformPutQuery - påkalles når klienten mottar data fra applikasjonen, altså fra forespørselen direkte
  * AppVersionResolver - hjelperklasse som kopler dataobjektnøkkel (k) med applikasjonsversjonens nøkkel (x) på formen `k + ":" + x`
  * ItemTransformerManager - hjelperklasse som kopler dataobjektnøkkel (k) med applikasjonsversjonens nøkkel (x) på formen `k + ":" + x`
* `package voldemort.client`
  * DBUpgradinatorStoreClient - Separat databaseklient som importerer de nye klassene fra DBUpgradinator - pakken
  * DBUpgradinatorStoreClientFactory (?)

Alternativt kan man benytte RMI - Remote Method Invocation, det vil si at hver enkelt databaseklient kontakter en kjørende "oppgraderingskoordinator".

## Om det fysiske perspektiv av arkitekturen
 * Hver enkelt av de fire applikasjonsnodene gjestes av DigitalOcean, og alle er spredd utover fire forskjellige datasentre i London, Amsterdam (to separate datasentre), og Frankfurt. Dette gjør at hver enkelt forespørsel til tjener kan gå omtrent like raskt med det gitte replikeringsskjema.
 * Replikeringsmønstre, 1 node per senter
   * AMS1 -> AMS1; LON1; FRA1
   * AMS2 -> AMS2; LON1; FRA1
   * LON1 -> LON1; AMS1; AMS2
   * FRA1 -> FRA1; AMS1; AMS2

## Testprossess
 * Total størrelse på testdata: 88 GB
 * Partisjon på hver instans: 22 GB; Replikert datavolum på hver instans: 44 GB; Volum per maskin: 66 GB
 * Gjenværende volum reservert OS og hurtigminne: 80 GB - 66 GB = 14 GB

## Trivia (Harry Potter-tema):
 * Brukernavn for UNIX-brukere: avery, black, crabbe, dolohov
 * IDer for lagringstjenere: riddle-diary, slytherin-locket, rawenclaw-diadem, nagini
