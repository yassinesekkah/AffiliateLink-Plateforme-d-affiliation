# Tests

Prerequis : Java 17 et Docker Desktop demarre (conteneurs Linux).
Le wrapper Maven est fourni : sous Windows, remplacer `mvn` par `.\mvnw.cmd` si Maven n'est pas installe.

```shell
mvn clean verify
```

Cette commande execute les tests unitaires `*Test` avec Surefire, puis les tests
Spring et PostgreSQL `*IT` avec Failsafe. Testcontainers gere les bases temporaires.
Flyway applique les migrations avant la validation du schema par Hibernate.

JaCoCo cumule la couverture des deux phases et genere :

- `target/site/jacoco/index.html` : rapport HTML.
- `target/site/jacoco/jacoco.xml` : rapport XML utilisable par SonarQube.
- `target/surefire-reports/` : resultats des tests unitaires.
- `target/failsafe-reports/` : resultats des tests d'integration.

Le build echoue si moins de 60 % des lignes sont couvertes. La classe principale,
les DTO et les configurations sont exclus. JaCoCo filtre automatiquement le code
annote comme genere (notamment Lombok).

Commandes ciblees :

```shell
mvn test
mvn test -Dtest=OfferServiceTest
mvn verify -Dit.test=OfferRepositoryIT
```

`mvn package` s'arrete avant les tests d'integration et le controle de couverture.
Utiliser `mvn clean verify` pour une validation complete.
