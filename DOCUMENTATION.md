# Bank System - Техническа документация

## 1. Описание на техническото решение

Системата е уеб базирано приложение за управление на клиенти, банкови сметки и кредити. Реализирана е с REST бекенд (Spring Boot) и React фронтенд, свързани чрез HTTP/JSON API.

---

## 2. Схема на базата данни

### Таблици и колони

#### `clients`
| Колона | Тип | Описание |
|---|---|---|
| `id` | BIGINT PK | Идентификатор |
| `client_type` | VARCHAR | Дискриминатор (`INDIVIDUAL` / `CORPORATE`) |

#### `individual_clients`
| Колона | Тип | Описание |
|---|---|---|
| `id` | BIGINT PK FK → clients | |
| `first_name` | VARCHAR | Име |
| `last_name` | VARCHAR | Фамилия |
| `egn` | VARCHAR(10) UNIQUE | ЕГН (10 цифри) |

#### `corporate_clients`
| Колона | Тип | Описание |
|---|---|---|
| `id` | BIGINT PK FK → clients | |
| `company_name` | VARCHAR | Наименование на фирмата |
| `eik` | VARCHAR(13) UNIQUE | ЕИК (9–13 цифри) |
| `representative_first_name` | VARCHAR | Представител - Име |
| `representative_last_name` | VARCHAR | Представител - Фамилия |

#### `bank_accounts`
| Колона | Тип | Описание |
|---|---|---|
| `id` | BIGINT PK | |
| `iban` | VARCHAR(34) UNIQUE | Номер на сметка |
| `balance` | DECIMAL(19,2) | Наличност |
| `status` | VARCHAR | `ACTIVE` / `CLOSED` |
| `client_id` | BIGINT FK → clients | Притежател |

#### `loan_types`
| Колона | Тип | Описание |
|---|---|---|
| `id` | BIGINT PK | |
| `category` | VARCHAR UNIQUE | `CONSUMER` / `MORTGAGE` |
| `annual_interest_rate` | DECIMAL(6,4) | Годишна лихва (напр. 0.0850) |
| `max_amount` | DECIMAL(19,2) | Максимална сума |
| `max_term_months` | INT | Максимален срок (месеци) |

#### `loans`
| Колона | Тип | Описание |
|---|---|---|
| `id` | BIGINT PK | |
| `client_id` | BIGINT FK → clients | Кредитополучател |
| `loan_type_id` | BIGINT FK → loan_types | Вид кредит |
| `amount` | DECIMAL(19,2) | Отпусната сума |
| `term_months` | INT | Срок за изплащане (месеци) |

#### `repayment_instalments`
| Колона | Тип | Описание |
|---|---|---|
| `id` | BIGINT PK | |
| `loan_id` | BIGINT FK → loans | |
| `month_number` | INT | Пореден номер на вноска |
| `total_payment` | DECIMAL(19,2) | Размер на вноската |
| `principal_part` | DECIMAL(19,2) | Главница |
| `interest_part` | DECIMAL(19,2) | Лихва |
| `remaining_balance` | DECIMAL(19,2) | Остатък след плащане |
| `paid` | BOOLEAN | Платена ли е |

### Релации

```
clients ──< individual_clients   (JOINED inheritance)
clients ──< corporate_clients    (JOINED inheritance)
clients ──< bank_accounts        (1:N, client_id)
clients ──< loans                (1:N, client_id)
loan_types ──< loans             (1:N, loan_type_id)
loans ──< repayment_instalments  (1:N, loan_id)
```

### Начални данни (data.sql)

При стартиране се зареждат два вида кредити, ако не съществуват:

| Вид | Лихва | Макс. сума | Макс. срок |
|---|---|---|---|
| CONSUMER | 8.50% | 50 000 BGN | 84 мес. |
| MORTGAGE | 4.50% | 500 000 BGN | 360 мес. |

---

## 3. Структура на проекта

```
Bank_System/
├── bank-service/                  # Spring Boot бекенд
│   └── src/main/java/org/example/bank_system/
│       ├── controller/            # REST контролери
│       │   ├── ClientController
│       │   ├── BankAccountController
│       │   └── LoanController
│       ├── service/               # Бизнес логика
│       │   ├── ClientService
│       │   ├── BankAccountService
│       │   └── LoanService
│       ├── entity/                # JPA ентити класове
│       │   ├── Client (абстрактен)
│       │   ├── IndividualClient
│       │   ├── CorporateClient
│       │   ├── BankAccount
│       │   ├── LoanType
│       │   ├── Loan
│       │   └── RepaymentInstalment
│       ├── repository/            # Spring Data JPA репозитори
│       ├── dto/
│       │   ├── request/           # Входни DTO (заявки)
│       │   └── response/          # Изходни DTO (отговори)
│       ├── exception/             # Обработка на грешки
│       │   ├── GlobalExceptionHandler
│       │   ├── BusinessRuleException
│       │   └── ResourceNotFoundException
│       └── config/
│           └── SecurityConfig
│
└── bank-ui/                       # React фронтенд
    └── src/src/
        ├── pages/
        │   ├── ClientsPage.tsx    # Управление на клиенти
        │   ├── AccountsPage.tsx   # Управление на сметки
        │   └── LoansPage.tsx      # Кредити и погасителен план
        ├── components/
        │   └── Layout.tsx         # Навигация и обвивка
        ├── api.ts                 # HTTP клиент функции
        └── types.ts               # TypeScript типове
```

---

## 4. Ключови компоненти от логиката

### 4.1 Наследяване на клиенти (JOINED Inheritance)

`Client` е абстрактен JPA ентити с JOINED стратегия. `IndividualClient` и `CorporateClient` го разширяват - данните им са в отделни таблици, свързани по `id`. Дискриминаторна колона `client_type` в `clients` указва вида.

### 4.2 Автоматично създаване на банкова сметка

При регистриране на нов клиент (физическо или юридическо лице) `ClientService` автоматично открива банкова сметка. IBAN се генерира като `BG00` + 16 hex символа от UUID - уникален и съответстващ на формата `[A-Z]{2}\d{2}[A-Z0-9]{1,30}`. Операцията е атомарна: ако създаването на клиента се провали, сметката не се записва.

### 4.3 Анюитетно погасяване

Изчислява се при отпускане на кредит в `LoanService.buildAnnuityPlan()`.

**Формула за месечна вноска:**

```
M = P × r / (1 − (1 + r)^(−n))
```

- `P` - главница (отпусната сума)
- `r` - месечна лихва = годишна лихва / 12
- `n` - брой месеци

**Разпределение по месец:**
- Лихва за месец k = оставаща главница × r
- Главница за месец k = M − лихва за месец k
- За последния месец главницата се изравнява с остатъка (елиминира натрупани грешки при закръгляне)
- Ако при последна вноска остатъкът е отрицателен (натрупана грешка), се хвърля `BusinessRuleException`
- Ако в междинен месец дялът главница е ≤ 0, се хвърля `BusinessRuleException`

Вноските са равни за целия срок. В началото преобладава лихвата, към края - главницата.

### 4.4 Бизнес правила

#### Отпускане на кредит (`LoanService.grantLoan`)
Преди създаване на кредит се проверява:
1. Заявената сума ≤ `LoanType.maxAmount`
2. Заявеният срок ≤ `LoanType.maxTermMonths`

При нарушение се хвърля `BusinessRuleException` (HTTP 422).

Фронтендът допълнително валидира и показва лимитите до полетата в реално време.

#### Закриване на сметка (`BankAccountService.closeAccount`)
Забранено е закриването на сметка с ненулева наличност. Ако `balance > 0`, се хвърля `BusinessRuleException` (HTTP 422).

#### Плащане на вноска (`LoanService.payInstalment`)
Заявката за четене на вноската от базата данни използва **песимистично заключване** (`PESSIMISTIC_WRITE`). Предотвратява двойно плащане при паралелни заявки.

#### Промяна на параметри на вид кредит (`LoanService.updateLoanType`)
Намаляването на `maxAmount` или `maxTermMonths` е забранено, ако съществуват активни кредити от този вид, чиято сума или срок надвишава новите лимити. При нарушение се хвърля `BusinessRuleException` (HTTP 422).

### 4.5 Обработка на грешки

`GlobalExceptionHandler` (`@RestControllerAdvice`) прихваща изключения и връща RFC 7807 Problem Details:

| Изключение | HTTP статус |
|---|---|
| `ResourceNotFoundException` | 404 |
| `BusinessRuleException` | 422 |
| `MethodArgumentNotValidException` | 400 |
| `NoResourceFoundException` | 404 |
| `Exception` (общо) | 500 |

### 4.6 REST API

| Метод | Endpoint | Описание |
|---|---|---|
| POST | `/api/clients/individual` | Добавяне на физическо лице (+ автоматична сметка) |
| POST | `/api/clients/corporate` | Добавяне на юридическо лице (+ автоматична сметка) |
| GET | `/api/clients/individual` | Списък физически лица |
| GET | `/api/clients/individual/{id}` | Физическо лице по ID |
| GET | `/api/clients/individual/by-egn?egn=` | Физическо лице по ЕГН |
| GET | `/api/clients/corporate` | Списък юридически лица |
| GET | `/api/clients/corporate/{id}` | Юридическо лице по ID |
| GET | `/api/clients/corporate/by-eik?eik=` | Юридическо лице по ЕИК |
| POST | `/api/accounts` | Ръчно откриване на сметка |
| PATCH | `/api/accounts/{id}/close` | Закриване на сметка (само при нулева наличност) |
| GET | `/api/accounts/{id}` | Сметка по ID |
| GET | `/api/accounts/client/{clientId}` | Всички сметки на клиент |
| POST | `/api/loans` | Отпускане на кредит |
| GET | `/api/loans/{id}` | Кредит по ID |
| GET | `/api/loans/client/{clientId}` | Кредити на клиент |
| GET | `/api/loans/{id}/repayment-plan` | Погасителен план |
| PATCH | `/api/loans/{loanId}/instalments/{month}/pay` | Отбелязване на платена вноска |
| GET | `/api/loans/{id}/status` | Статус на кредит (платени/общо вноски) |
| GET | `/api/loans/types` | Видове кредити |
| PATCH | `/api/loans/types/{id}` | Промяна на параметри на вид кредит |

---

## 5. Използвани технологии

### Бекенд
| Технология | Версия | Роля |
|---|---|---|
| Java | 21 | Език за програмиране |
| Spring Boot | 4.x | Уеб рамка, IoC контейнер |
| Spring Data JPA | - | ORM слой |
| Spring Security | - | Конфигурация на сигурността |
| Spring Validation | - | Валидиране на входни данни |
| Hibernate | - | JPA имплементация |
| PostgreSQL | 15+ | Релационна БД (продукция) |
| H2 | - | In-memory БД (тестове) |
| Lombok | - | Намаляване на шаблонен код |
| JUnit 5 | - | Unit и интеграционни тестове |
| Mockito | - | Мокване при unit тестове |
| Spring MockMvc | - | HTTP слой при интеграционни тестове |
| JsonPath | 2.x | Парсване на JSON отговори в тестове |
| Maven | 3.x | Изграждане на проекта |

### Фронтенд
| Технология | Версия | Роля |
|---|---|---|
| React | 18 | UI рамка |
| TypeScript | 5.x | Типизиран JavaScript |
| PrimeReact | - | UI компонент библиотека |
| PrimeFlex | - | CSS utility класове |
| Create React App | - | Конфигурация и dev сървър |

---

## 6. Тестове

### Unit тестове (`src/test/java/.../service/`)

| Клас | Покрива |
|---|---|
| `ClientServiceTest` | Създаване на клиент, дублирано ЕГН/ЕИК, автоматична сметка |
| `BankAccountServiceTest` | Откриване/закриване, ненулева наличност, дублиран IBAN |
| `LoanServiceTest` | Отпускане, валидация на лимити, плащане, статус, промяна на вид |
| `LoanServiceAnnuityTest` | Анюитетни изчисления - коректност на план, последна вноска |

Използват Mockito - без реална база данни.

### Интеграционни тестове (`src/test/java/.../integration/`)

| Клас | Покрива |
|---|---|
| `ClientControllerIT` | Всички клиентски endpoints, авто-сметка, валидации |
| `BankAccountControllerIT` | Всички сметкови endpoints, бизнес правила |
| `LoanControllerIT` | Всички кредитни endpoints, погасителен план, статус |

Стартират пълен Spring контекст с H2 in-memory БД. `MockMvc` изпраща реални HTTP заявки към контролерите. Базата се изчиства преди всеки тест в правилния FK ред.

**Изпълнение:**
```bash
# Unit тестове
./mvnw -pl bank-service test

# Интеграционни тестове
./mvnw -pl bank-service test -Dtest="*IT"

# Всички тестове
./mvnw -pl bank-service test -Dtest="*IT,*Test"
```

---

## 7. Използвани източници

- Spring Boot документация - https://docs.spring.io/spring-boot/docs/current/reference/html/
- Spring Data JPA документация - https://docs.spring.io/spring-data/jpa/docs/current/reference/html/
- Jakarta Validation спецификация - https://beanvalidation.org/
- React документация - https://react.dev/
- PrimeReact документация - https://primereact.org/
- RFC 7807 (Problem Details for HTTP APIs) - https://www.rfc-editor.org/rfc/rfc7807
- Анюитетна формула - https://en.wikipedia.org/wiki/Annuity

---

## 8. Задачи на участниците в екипа

| Участник            | Изпълнени задачи                                              |
|---------------------|---------------------------------------------------------------|
| Леис Абдулах        | Разработка на база данни и UI компоненти                      |
| Момчил Цанов        | Разработка на клиент логика и потребителски интерфейс         |
| Християн Иванов     | Реализиране на модели на банкови сметки и погасителни планове |
| Ивайло Стойков      | Реализиране на моделите на различните кредити и бизнес логика |
| Владимир Владимиров | Integration тестове и бизнес логика по моделите               |
