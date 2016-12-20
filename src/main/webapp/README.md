# Search client for Marcus
Denne "versjonen" bruker `npm` til å hente Semantic-UI og `gulp` til å bygge et eget uib theme.

**uib** theme er under git kontroll, siden den ligger uten for `/semantic` mappen. 

`npm install` gjør følgende i en *postinstall* prosess:

1. `default.theme.config` og `default.semantic.json` blir kopiert til `/semantic/src/`
2. `semantic/src/themes/default` kopiert til `/themes` (i `/themes` blir alle themes ignorert bortsett fra `uib`)

Se `package.json` for info.

## Installering

```bash
git clone git@gitlab.com:ubbdev/marcus-search-client.git
npm install semantic-ui 

# GJØR DETTE!
# ? It looks like you have a semantic.json file already. Yes, extend my current settings.
# ? Set-up Semantic UI Automatic (Use defaults locations and all components)
# ?
#    We detected you are using NPM. Nice!
# 
#    Is this your project folder?
#    /Users/tarjelavik/git/marcus-search-client
# 
# Yes
#? Where should we put Semantic UI inside your project? semantic/

npm install
cd semantic/
gulp build
```

## Oppgradering av Semantic UI
Dersom Semantic UI blir oppdatert kan **default** i `/themes` bli ute av sync med `/semantic/src/themes/default`.

```bash
rm -r semantic
npm install semantic-ui -s
npm install
```

## TODO
- [x] Styling
- [x] Responsive
- [x] Menyer/sidebars for hovednavigasjon
- [x] Menyer/sidebars for fasetter
- [x] Datosøk fungerer ikke med sidebar óg kolonne
- [x] Legge inn footer
- [ ] Søkebok plukker ikke alltid opp søkestrengen i ?q

### SimpleCart.js 
@todo

Kan vi bruke simleCart her? Vi har vel ikke tilgang til cart-info? Eller kanskje når vi får det på server? Får se