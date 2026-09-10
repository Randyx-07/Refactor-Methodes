# Journal de refactoring

| Classe/méthode    | Problème observé  | Refactoring appliqué          | Justification                                                                    |
|-------------------|-------------------|-------------------------------|----------------------------------------------------------------------------------|
| canCarryHazardous | Trop de if manuel | Remplacer par un test bitwise | Le code est moins long mais gère toujours toutes les combinaisons de permissions |
