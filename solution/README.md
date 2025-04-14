Протестировать Telegram бота можно [здесь](https://t.me/advertee_bot).

# advertee 💜 - платформа для размещения рекламы

# Оглавление

- [Инструкция по запуску](#инструкция-по-запуску)
- [Работа алгоритма подбора рекламы](#работа-алгоритма-подбора-рекламы)
- [API](#api)
- [Картинки для кампаний](#картинки-для-кампаний)
- [Генерация рекламных объявлений](#генерация-рекламных-объявлений)
- [Модерация](#модерация)
- [Telegram бот](#telegram-бот)
- [Визуализация статистики](#визуализация-статистики)
- [Метрики](#метрики)
- [Дашборды](#дашборды)
- [Стек технологий](#стек-технологий)
- [Тестирование](#тестирование)
- [Устройство Docker Compose](#устройство-docker-compose)
- [Устройство базы данных](#устройство-базы-данных)

## Инструкция по запуску

Для запуска необходимо установить Docker.

### 1. Склонируйте репозиторий

```bash
git clone https://gitlab.prodcontest.ru/2025-final-projects-back/Neruxov/
```

### 2. Перейдите в папку с решением

```bash
cd Neruxov/solution
```

### 3. Запустите с помощью Docker Compose

#### Переменные окружения уже заполнены за Вас

```bash
docker compose up -d
```

### Готово! Приложение (API) и Telegram бот запущены и готовы к использованию.

## Работа алгоритма подбора рекламы

Если вы не видите формулы, поставьте, пожалуйста, темную тему!

Важно: Во всех случаях, если не указано другое условие, нормализация происходит по `min/max`, где `min` = 1% перцентиль,
`max` = 99% перцентиль (по всей базе данных)

Из БД получаем уже отсортированные по таргетингу, указанному началу и концу кампании, а также лимиту по показам
кампании.

Cчитаем, что условие по показам: `(current_impressions + 1) / impressions_limit < 1.05`, чтобы подзаработать на
отсутствии штрафа.

Потом считаем для каждой кампании `score`, сортируем по убыванию и возвращаем первое значение из отсортрованного
списка.

### Как считается score?

![score_formula.png](assets/algorithm/score_formula.png)

1. `ml_score` - нормализованные ML скоры
2. `profit` - нормализованная прибыль, считается по формуле:

![profit_formula.png](assets/algorithm/profit_formula.png)

`conversion` - если у кампании более 50 показов, считается по ее данным, иначе по рандомной выборке из 250 действий (
других кампаний).

3. `fulfillment_factor` - нормализованная (в данном случае по min/max на **текущих** данных) сумма ошибок по показам и
   кликам на текущий день

![ff_formula.png](assets/algorithm/ff_formula.png)

![re_formula.png](assets/algorithm/re_formula.png)

`expected` - цель на сегодняшний день, = `limit` / `days_total` * `days_passed` (считаем, что каждый день будет
одинаковое кол-во действий)

`actual` - текущее кол-во действий

## API

### Swagger доступен по адресу [http://localhost:8080/docs/swagger-ui/index.html](http://localhost:8080/docs/swagger-ui/index.html)

<details>
<summary>Демонстрация работы основных методов</summary>

#### Создание и получение клиентов

<img src="assets/demo/clients_bulk.png" alt="clients_bulk" width="600px">
<img src="assets/demo/client_get.png" alt="client_get" width="600px">

#### Создание и получение рекламодаталей

<img src="assets/demo/advertisers_bulk.png" alt="advertisers_bulk" width="600px">
<img src="assets/demo/get_advertiser.png" alt="get_advertiser" width="600px">

#### Запись ML-скора

<img src="assets/demo/ml_scores.png" alt="ml_scores" width="600px">

#### CRUD кампаний

<img src="assets/demo/campaign_create.png" alt="campaign_create" width="600px" width="600px">
<img src="assets/demo/delete_campaign.png" alt="delete_campaign" width="600px">
<img src="assets/demo/get_campaign.png" alt="get_campaign" width="600px">
<img src="assets/demo/get_campaigns.png" alt="get_campaigns" width="600px">
<img src="assets/demo/update_campaign.png" alt="update_campaign" width="600px">

#### Статистика

<img src="assets/demo/advertiser_stats.png" alt="advertiser_stats" width="600px">
<img src="assets/demo/advertiser_stats_daily.png" alt="advertiser_stats_daily" width="600px">
<img src="assets/demo/campaign_stats.png" alt="campaign_stats" width="600px">
<img src="assets/demo/campaign_stats_daily.png" alt="campaign_stats_daily" width="600px">

#### Получение рекламы и клик на неё

<img src="assets/demo/get_ad.png" alt="get_ad" width="600px">
<img src="assets/demo/click_ad.png" alt="click_ad" width="600px">

#### Смена даты

<img src="assets/demo/time_advance.png" alt="time_advance" width="600px">

</details>

Ниже кратко расписаны эндпоинты дополнительных фич и идеи их реализации.

## Картинки для кампаний

Загрузка происходит через отдельные эндпоинты:
- `POST /attachments/advertisers/{advertiserId}` - загрузка картинки (передавать файл через multipart, параметр `file`)
- `GET /attachments/advertisers/{advertiserId}` - получение всех картинок рекламодателя с пагинацией (параметры: `size`,
  `page`, необязательны)
- `GET /attachments/{attachmentId}` - получение картинки по id
- `GET /attachments/{attachmentId}/content` - скачивание картинки по id (параметр: `download` = true / false)
- `DELETE /attachments/{attachmentId}` - удаление картинки по id

<details>
<summary>Демонстрация работы</summary>

<img src="assets/demo/attachment_content.png" alt="attachment_content" width="600px">
<img src="assets/demo/attachment_delete.png" alt="attachment_delete" width="600px">
<img src="assets/demo/attachment_get.png" alt="attachment_get" width="600px">
<img src="assets/demo/attachment_list.png" alt="attachment_list" width="600px">
<img src="assets/demo/attachment_upload.png" alt="attachment_upload" width="600px">

</details>

Есть ограничения:

- только `GIF`, `JPG (JPEG)`, `PNG`
- до `512 КБ` (вывод сделан из ограничений Google AdSense - `150 КБ` и Яндекс Директа - `300 КБ`, с запасом)

Картинки хранятся в облаке (Yandex Cloud Object Storage), а в базе данных хранится только их ключ для получения.
Их можно добавить к кампании через поле `attachment_id`. Удалить картинку можно только в том случае, когда она не
привязана ни к какой кампании.

При получении рекламы (GET /ads), возвращается также поле `attachment_id` ("фронтенд должен подгрузить ее через
`GET /attachments/{attachmentId}/content`")

## Генерация рекламных объявлений

- `POST /ai/ad-content` - генерация рекламного объявления (JSON тело запроса, `advertiser_id`, `request` - "промпт" для
  генерации). В ответ получаем JSON объект, с полями: `text`, `title`, `rejected` = true / false.

<details>
<summary>Демонстрация работы</summary>

<img src="assets/demo/ai_ad_content.png" alt="ai_ad_content" width="600px">

</details>

Генерация рекламных объявлений происходит с помощью модели `gpt-4o-mini` от OpenAI. (промпт и json-схема находятся в
src/main/resources/generation).

Модель "модерирует" запрос. Если в ответе получаем `rejected` = `true`, то `text` и `title` будут пустыми - модель
отказалась генерировать данную кампанию.

Модель также пытается имитировать стиль рекламодателя. В запросе ей передаются до 5 текстов других кампаний
рекламодателя, чтобы модель могла на них ориентироваться.

## Модерация

#### Включение модерации:

- `POST /options/moderation` (JSON тело запроса, `image_enabled` = true / false,
  `text_enabled` = true / false)

<details>
<summary>Демонстрация работы</summary>

#### Настройки модерации

<img src="assets/demo/options_moderation.png" alt="options_moderation" width="600px">

#### Ручная модерация

<img src="assets/demo/review_request_reject.png" alt="review_request_reject" width="600px">
<img src="assets/demo/review_request_approve.png" alt="review_request_approve" width="600px">
<img src="assets/demo/review_request_get.png" alt="review_request_get" width="600px">
<img src="assets/demo/review_request_list.png" alt="review_request_list" width="600px">

#### Загрузка некорректной картинки

<img src="assets/demo/attachment_upload_inappr.png" alt="attachment_upload_inappr" width="600px">

</details>

Для модерации используется модель `gpt-4o-mini` от OpenAI. (промпт и json-схема находятся в
src/main/resources/moderation).
При создании кампании, если модерация включена, кампания отправляется на модерацию в фоне, и через какое-то время ее
`moderation_status` изменится с `AWAITING_MODERATION` на `APPROVED`, `REJECTED` или `ON_MANUAL_REVIEW`. Если модерация
выключена - `UNMODERATED`.

![moderation_status.png](assets/demo/moderation_status.png)

Если статус `ON_MANUAL_REVIEW`, то модель посчитала, что объявление требует ручной модерации. "Администратор" может либо
одобрить, либо отклонить кампанию через данные эндпоинты:

- `GET /review-requests/{id}` - получение запроса на модерацию по id
- `GET /review-requests` - получение всех запросов на модерацию с пагинацией (параметры: `size`, `page`, необязательны)
- `POST /review-requests/{id}/approve` - одобрение кампании по id запроса на модерацию
- `POST /review-requests/{id}/reject` - отклонение кампании по id запроса на модерацию

На данный момент, данный статус ни на что не влияет. Но, в реальном приложении, кампании не запускались бы до
прохождения
модерации.

Помимо модерации текста кампаний, можно включить модерацию картинок. Она выполняется при загрузке изображения, и если
картинка не прошла модерацию, вы сразу получите ошибку.

**Пожалуйста, не загружайте много картинок на модерацию 🙏**

## Telegram бот

#### Через Telegram бота рекламодатели могут создавать кампании, редактировать и удалять их, а также смотреть статистику, в том числе и по дням.

<details>
<summary> Регистрация (либо вход в уже существующий аккаунт по UUID / названию) </summary>

<img src="assets/telegram/telegram_signup.gif" alt="Регистрация"/>
</details>

<details>
<summary>Создание кампании</summary>

<img src="assets/telegram/telegram_new_campaign.gif" alt="Создание кампании"/>
</details>

<details>
<summary>Редактирование кампании</summary>

<img src="assets/telegram/telegram_edit_campaign.gif" alt="Редактирование кампании"/>
</details>

<details>
<summary>Удаление кампании</summary>

<img src="assets/telegram/telegram_delete_campaign.gif" alt="Удаление кампании"/>
</details>

<details>
<summary>Изменение даты</summary>

<img src="assets/telegram/telegram_time_advance.gif" alt="Изменение даты"/>
</details>

## Метрики

### Есть 5 основных метрик:

- `advertee_business_daily_count` - количество показов/кликов по дням (теги: `campaign`, `day`, `advertiser`)
- `advertee_business_daily_revenue` - сумма, заработанная с показов/кликов по дням (теги: `campaign`, `day`,
  `advertiser`)
- `advertee_business_campaigns_count` - количество кампаний
- `advertee_business_active_campaigns` - количество активных кампаний
- `advertee_review_requests_active` - количество неотвеченных заявок на модерацию

## Дашборды

### Графана доступна по адресу [http://localhost:3000](http://localhost:3000)

### Данные для входа: `admin` / `admin`

### Со стороны бизнеса

Показана прибыль платформы, количество показов/кликов по дням, количество активных и всего кампаний, а также количество
неотвеченных заявок на модерацию.

![grafana_business.png](assets/grafana/grafana_business.png)

### Со стороны рекламодателя

Есть такая же статистика по дням, но для выбранной(-ых) кампаний, а так же возможность смотреть общую статистику за
какой-то период.

![grafana_advertiser_daily.png](assets/grafana/grafana_advertiser_daily.png)
![grafana_advertiser_total.png](assets/grafana/grafana_advertiser_total.png)

### Со стороны разработчика

Взяты публичные дэшборды для мониторинга производительности приложения.

![grafana_jvm.gif](assets/grafana/grafana_jvm.gif)
![grafana_throughput.gif](assets/grafana/grafana_throughput.gif)

## Тестирование

Есть и Unit, и E2E тесты. Запустить их можно через Gradle:

```bash
./gradlew test
```

Для Test Coverage используется `JaCoCo` (результаты в `jacoco/index.html`). Процент покрытия (instructions /
branches):

Сервисы: `85%` / `67%`

Контроллеры: `79%` / `N/A`

Общее покрытие: `81%` / `64%`

В папке `algorithm_test` также есть тест алгоритма подбора рекламы, написанный на скорую руку (и переписанный полностью
ChatGPT в момент написания этого ридми) чисто для оценки эффективности алгоритма на п*тоне. Пожалуйста, не судите по
нему чистоту кода 🙏

## Стек технологий

### Рантайм

- Kotlin
- Spring Boot
- Spring Boot Web
- Spring Boot Data JPA + PostgreSQL
- Spring Boot Data Validation
- Spring Boot Data Cache + Caffeine
- Spring Boot AI (OpenAI)
- Spring Boot Actuator + Micrometer + Prometheus
- AWSpring S3 + Yandex.Cloud Object Storage
- [Telegram Bot Library](https://github.com/vendelieu/telegram-bot)

<details>
<summary>Обоснование выбора</summary>

#### Почему OpenAI?

OpenAI имеет лучшее соотношение цена/качество среди разных LLM моделей. `gpt-4o-mini` отлично подходит для генерации
рекламных кампаний и их модерации со своей низкой ценой.

В отличие от модерации по блеклисту, модель способна обнаруживать скрытые смыслы и завуалированные слова, что делает ее
эффективнее и успешнее.

#### Почему PostgreSQL?

Данные строго структурированные, поэтому реляционная база данных подходит лучше всего. PostgreSQL - это популярная и
надежная реляционная база данных, с большим набором инструментов хранения и обработки данных.

#### Почему Prometheus?

Prometheus - это популярный инструмент для мониторинга, который хорошо интегрируется с Spring Boot и имеет удобный язык
запросов PromQL, позволяющий легко получать необходимые данные.

#### Почему S3 и, в частности, Yandex.Cloud Object Storage?

S3 - это популярный, надежный и масштабируемый сервис для хранения файлов. Yandex.Cloud Object Storage - это
альтернатива S3 от Amazon, с дешевыми тарифами, совместимостью с AWS S3 API и датацентрами в России.

#### Почему именно эта библиотека для Telegram бота?

Эта библиотека позволяет легко создавать ботов на Kotlin, имеет удобный DSL для создания команд и обработчиков.

</details>

### Тесты

- JUnit 5 + Kotest
- Testcontainers (LocalStack + Postgres)
- MockK
- [Stove E2E](https://github.com/Trendyol/stove)

<details>
<summary>Обоснование выбора</summary>

#### Почему Testcontainers?

Testcontainers - это удобный инструмент для запуска контейнеров во время тестов, что позволяет тестировать приложение в
реальном окружении, с поддержкой множества баз данных и сервисов (например, S3).

#### Почему Stove E2E?

Stove E2E - это удобный инструмент для тестирования API, который позволяет писать тесты на Kotlin DSL, что делает их
более читаемыми и понятными.

</details>

### Локальная разработка

- Spring Boot Docker Compose (файл: `dev-compose.yml`)

### Визуализация статистики

- Grafana

<details>
<summary>Обоснование выбора</summary>

#### Почему Grafana?

Grafana - это популярный и удобный инструмент для визуализации данных, который позволяет через интуитивно-понятный
интерфейс создавать дашборды и интегрируется с Prometheus.

</details>

## Устройство Docker Compose

### В файле `docker-compose.yml` описаны четыре сервиса:

1. `postgres` (база данных)
2. `app` (запускает и API приложения, и Telegram бота, дожидаясь запуска `postgres`)
3. `prometheus` (сборщик метрик)
4. `grafana` (визуализатор метрик, дожидается запуска `prometheus`)

#### На каждый прописаны свои переменные окружения, а также настроены health-чеки и volumes.

#### `app` собирается из `Dockerfile`'а, в котором есть три стадии:

1. Скачивание зависимостей (данный слой кешируется, тем самым ускоряя последующие сборки)
2. Сборка приложения
3. Запуск приложения

## Устройство базы данных

![schema.png](assets/db/schema.png)
