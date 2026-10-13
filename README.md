# MineNorth Drogue

Mod Forge 1.20.1 (`minenorthdrogue`), sans dépendance. Remplace Narcosis : marijuana, cocaïne, méthamphétamine, avec production et lien avec le mod Police.

## Production
- **Cultures** (graines plantées sur terre labourée, 4 stades, récolte au stade 3 : feuilles + graines) : `cannabis_seeds` → feuille de cannabis, `coca_seeds` → feuille de coca, `ephedra_seeds` → éphédra.
  Les graines ne se craftent pas : le staff les donne (créatif / commande) ou les vend via le mod Boutiques.
- **Produit chimique** (`chemical`) : poudre à canon + poudre d'os + fiole.
- **Joint** : 3 feuilles de cannabis + papier → 2 joints (établi).
- **Laboratoire clandestin** (`lab`) : clic droit pour y poser les ingrédients un à un ; la fabrication démarre quand la recette est complète, le produit se récupère à main nue (accroupi, main vide : reprendre les ingrédients).
  - Cocaïne : 4 feuilles de coca + 1 produit chimique, 30 s, 2 doses.
  - Méthamphétamine : 3 éphédra + 1 produit chimique + 1 redstone, 45 s, 2 doses.

## Effets (positifs, limités par un délai d'attente)
| Substance | Effets par défaut | Délai avant la dose suivante |
|---|---|---|
| Joint | Régénération I, 20 s | 2 min |
| Cocaïne | Vitesse I + Hâte I, 60 s | 4 min |
| Méthamphétamine | Vitesse II + Hâte II + Résistance I, 45 s | 6 min |

Tout est réglable dans `config/minenorth_drogue.toml` (serveur dédié) : lignes `effet;secondes;niveau`, délais, durées et rendements du laboratoire.

## Lien avec le mod Police
Chaque prise est notée dans les données du joueur (`minenorthpolice_drogues`, format lu par le test salivaire du mod Police) : le test salivaire est positif pendant `drogue_detection_minutes` et affiche le nom de la substance (Joint, Cocaïne, Méthamphétamine).

## Compiler
Java 17 (Gradle 8.8 ne tourne pas sur un JDK trop récent : `JAVA_HOME` sur un JDK 17 à 21), puis `./gradlew build`.
