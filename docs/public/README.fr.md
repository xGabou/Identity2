# Identity2

Identity2 est un mod de morph/identite base sur Architectury pour Minecraft `1.21.11`, cible pour Fabric et NeoForge.

Il permet de debloquer des entites, se transformer en celles-ci, utiliser des capacites propres a chaque identite, et conserver les donnees de variante (par exemple la couleur du mouton) pour que le rendu corresponde a la variante choisie.

## Points Forts

- Systeme de morph avec progression de debloquage (`kill -> unlock -> morph`).
- Menu d'identite cote client avec recherche et filtres.
- Decouverte dynamique des variantes avec fallback securise.
- Support de variantes connues pour mouton, axolotl et chat.
- Detection generique des variantes numeriques pour les entites moddees (scan borne et securise).
- Capacites d'identite data-driven via le registre `identity2:identity_ability`.
- Sync de la forme (`width_override`, `height_override`) et de la hauteur des yeux.
- Gestion du vol pour les identites volantes sans casser les comportements creative/spectator.
- Sync reseau du morph et des donnees custom pour tous les joueurs qui trackent l'entite.

## Controles

Raccourcis client par defaut :

- Ouvrir le menu d'identite : `G`
- Utiliser la capacite d'identite : `V`

## Commandes

Namespace principal :

- `/identity morph <identity_id>`
- `/identity clear`
- `/identity list`

Notes :

- Le morph verifie que l'identite est morphable.
- Si `requireUnlockedIdentityForMorph` est active, les non-op doivent avoir debloque l'identite.

## Variantes d'Identite

Les variantes sont stockees et synchronisees via :

- Type de base : `identity2.identity_type`
- Payload de variante : `identity2.identity_variant`

Quand une variante est selectionnee, son NBT est applique a l'instance d'identite creee afin que le modele/texture/couleur correspondent.

Si aucune variante n'est decouverte, l'UI retombe proprement sur une entree unique par defaut.

## Prerequis de Build

- Java `21`
- Gradle Wrapper (inclus)

Build rapide :

```bash
./gradlew build
```

Windows :

```powershell
.\gradlew.bat build
```

## Lancement en Dev

Taches typiques :

```bash
./gradlew :fabric:runClient
./gradlew :neoforge:runClient
```

## Dependances

Principales :

- Architectury API
- Fabric API (cote Fabric)
- gabous-libs

## Licence

Tous droits reserves. Voir `LICENSE.txt`.
