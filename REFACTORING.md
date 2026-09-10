# Journal de refactoring

| Classe/méthode                      | Problème observé                             | Refactoring appliqué                  | Justification                                                                     |
|-------------------------------------|----------------------------------------------|---------------------------------------|-----------------------------------------------------------------------------------|
| canCarryHazardous                   | Trop de if manuel                            | Remplacer par un test bitwise         | Le code est moins long mais gère toujours toutes les combinaisons de permissions  |
| validateCalculatePrintSaveAndNotify | La méthode fais trop de choses en même temps | création d'une méthode calculateTotal | le calcul du prix possède maintenant sa propre responsabilité                     |
