# google-cloud-proxy

Тонкий Ktor-прокси к Google Cloud Text-to-Speech / Speech-to-Text.
Ваш Android-клиент (или backend) обращается к этому сервису, а не напрямую к Google —
ключ API нигде не попадает на 150 панелей, и контракт совпадает по духу с
`piper-server` / `whisper-ktor`.

## Структура (Clean Architecture)

```
domain/                     — интерфейсы TextToSpeechProvider / SpeechToTextProvider (ни строчки Google Cloud)
data/googlecloud/           — единственное место, знающее про формат запросов Google
presentation/routes/        — HTTP-роуты, зависят только от domain-интерфейсов
di/                         — Koin-модуль, связывает интерфейсы с реализациями
config/                     — чтение переменных окружения
```

Если позже понадобится второй провайдер (например, для A/B с self-hosted) —
это новая реализация `TextToSpeechProvider`/`SpeechToTextProvider` и одна
дополнительная строка в `di/AppModule.kt`. Роуты и контракт для клиента не меняются.

## Запуск локально в Android Studio

1. `File > Open` → выбрать папку `google-cloud-proxy` (Android Studio откроет её
   как обычный Kotlin/JVM Gradle-проект — Android SDK для этого не нужен).
2. Дать Gradle синхронизироваться (подтянет Ktor/Koin из Maven Central).
3. `Run > Edit Configurations` → добавить Kotlin-конфигурацию с main-классом
   `com.nurtelecom.nurai.googlecloudproxy.ApplicationKt`, в Environment variables указать:
   ```
   GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/to/google-cloud-proxy/secrets/your-key.json
   ```
   (Service Account JSON-ключ кладите в `secrets/` — папка в `.gitignore`, в git не попадёт.
   `PROXY_AUTH_TOKEN` можно не указывать — тогда проверка токена отключена, удобно для локальных тестов)
4. Запустить — поднимется на `http://localhost:8003`.

## Быстрая проверка

```bash
curl http://localhost:8003/health

# voiceName явно — конкретная модель голоса
curl -X POST http://localhost:8003/tts \
  -H "Content-Type: application/json" \
  -d '{"text":"Merhaba, size nasıl yardımcı olabilirim?","languageCode":"tr-TR","voiceName":"tr-TR-Wavenet-D"}' \
  --output test.ogg

# voiceName опущен — Google сам подбирает голос под language+gender (?gender=FEMALE|MALE|NEUTRAL, дефолт FEMALE)
curl -X POST "http://localhost:8003/tts?gender=FEMALE" \
  -H "Content-Type: application/json" \
  -d '{"text":"Hallo, wie kann ich Ihnen helfen?","languageCode":"de-DE"}' \
  --output test-de.ogg

curl -X POST "http://localhost:8003/stt?lang=ru-RU&alt=ky-KG,tr-TR" \
  -H "Content-Type: application/octet-stream" \
  --data-binary @sample.ogg
```

Список голосов (`voiceName`) для нужного языка — `gcloud` или REST-эндпоинт
`voices:list`, чтобы подобрать конкретное имя турецкого голоса под тест.
Без `voiceName` Google сам выбирает голос по `languageCode` + `?gender=` —
даёт консистентное звучание между языками без привязки к конкретной модели.

## Деплой на VPS

Ключ (`secrets/nurai-gcp-key.json`) в `.gitignore` — в GitHub не попадёт.
Значит на VPS его нужно класть отдельно, в обход git, каждый раз вручную.

1. **В GitHub** — обычный push всего `google-cloud-proxy/`, ключ туда не уйдёт
   (проверено: `git status --ignored` показывает `secrets/` как ignored).

2. **На VPS — `git pull` в папку, где лежат остальные сервисы** (рядом с
   `whisper-ktor`/`piper-server`/`akylai-server`), затем `mkdir -p google-cloud-proxy/secrets`.

3. **Ключ — отдельным `scp` с Mac, напрямую на VPS**, не через git:
   ```bash
   scp secrets/nurai-gcp-key.json <ssh-user>@<vps-ip>:<путь-к-репозиторию>/google-cloud-proxy/secrets/nurai-gcp-key.json
   ```

4. **В `.env` рядом с `docker-compose.yml` на VPS** — добавить (если ещё нет):
   ```
   PROXY_AUTH_TOKEN=<придумать_токен>
   ```

5. **Вставить блок сервиса** из `docker-compose.snippet.yml` в существующий
   `docker-compose.yml` на VPS (просто скопировать, ничего в нём переименовывать не надо —
   путь к ключу там уже указывает на `google-cloud-proxy/secrets/nurai-gcp-key.json`).

6. **Собрать и поднять только этот сервис**, не трогая остальные:
   ```bash
   docker compose build google-cloud-proxy
   docker compose up -d google-cloud-proxy
   ```

7. **Проверить изнутри VPS** (сервис слушает `8003` только на docker-сети,
   наружу — только через nginx, см. ниже):
   ```bash
   docker compose exec google-cloud-proxy wget -qO- http://localhost:8003/health
   docker compose logs -f google-cloud-proxy
   ```

Ключ на образ не запекается (см. `.dockerignore`), в контейнер попадает только
через read-only bind-mount из шага 3+5.

## На что смотреть при сравнении с self-hosted

Ответ на `/tts` и `/stt` содержит время обработки на стороне Google
(`X-Processing-Time-Ms` заголовок для TTS, `processingTimeMs` в JSON для STT) —
это время, потраченное конкретно на вызов Google, без учёта сети до вашего клиента.
Так вы сможете честно разложить общую задержку на "сеть" и "инференс" и сравнить
с тем же замером на `whisper-ktor`/`piper-server`.

## Ограничение, о котором нужно помнить

`/stt` использует синхронный `speech:recognize` — до ~1 минуты аудио, не потоковый.
Для коротких голосовых фраз (несколько секунд) этого достаточно. Если понадобится
именно потоковое распознавание в реальном времени — у Google это отдельный
gRPC-based streaming API, реализация будет другой (но так же за интерфейсом
`SpeechToTextProvider`, роуты не тронутся).
