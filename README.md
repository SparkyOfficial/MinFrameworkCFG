# Mini Config Framework (Java)

Простая библиотека для централизованного управления настройками с поддержкой YAML, JSON и переменных окружения, плюс горячая перезагрузка.

## Возможности
- YAML + JSON + ENV как источники (`ConfigSource`)
- Глубокое слияние (последние источники перекрывают предыдущие)
- Доступ по dot-path (`"db.host"`), вложенные структуры
- Типизированные геттеры и базовая проверка типов
- Горячая перезагрузка файлов через WatchService

## Установка
Maven (Java 11+):
```xml
<dependency>
  <groupId>com.sparky</groupId>
  <artifactId>mini-config</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Быстрый старт
```java
import com.sparky.config.ConfigManager;
import com.sparky.config.source.*;
import java.nio.file.Path;

try (var config = ConfigManager.builder()
        .addSource(new YamlConfigSource(Path.of("src/main/resources/application.yml")))
        .addSource(new JsonConfigSource(Path.of("src/main/resources/application.json")))
        .addSource(new EnvConfigSource("APP_")) // env c префиксом APP_
        .hotReload(true)
        .build()) {

    String host = config.getString("db.host");
    int port = config.getInt("db.port");
    boolean debug = config.getBool("app.debug");

    // вложенные структуры
    var creds = config.get("db.credentials", new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>(){});

    // подписка на обновления
    config.addChangeListener(map -> System.out.println("Config updated: " + map.keySet()));
}
```

## Приоритеты
Порядок добавления источников важен: более поздние перекрывают более ранние. Обычно: `YAML -> JSON -> ENV`.

## ENV правила
- Необязательный префикс, например `APP_`
- Двойное подчеркивание `__` превращается в точку: `APP_DB__HOST` → `db.host`
- Ключи приводятся к нижнему регистру

## Примеры
`src/main/resources/application.yml`
```yaml
app:
  name: demo
  debug: true

db:
  host: localhost
  port: 5432
  credentials:
    user: user
    pass: pass
```

`src/main/resources/application.json`
```json
{
  "app": { "debug": false },
  "db": { "port": 5433 }
}
```

## Лицензия
MIT
