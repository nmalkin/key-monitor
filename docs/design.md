Key Monitor design
==================

# Components
The required functionality can be broken down into 6 components:

## Sign-up service
Listens for new messages to the public-facing number. When someone messages it, registers them as a user (i.e., remembers that their keys need to be monitored) and sends a confirmation message to the email they provided.

### Data model
#### users
- phone number
- registration time
- initial key
- status (active / inactive) - whether we're actively monitoring their keys or not
#### emails
- user
- email address
- email verification status: unverified / verified
- email status (should we use it): active, removed

## Unsubscribe service
The registration email (and any notifications) will include an unsubscribe link. We need a very basic web service that performs the unsubscribe action if the user clicks on the link in that email.

## Scheduling service
We need to retrieve the monitored keys at least once an hour. To avoid overloading the Signal server, we shouldn't perform the requests all at once. Instead, we’ll spread them randomly throughout the hour.

The scheduling task will run once an hour to determine when exactly each key retrieval should happen. It will add a row to the database for each retrieval required. The rows must be in the order in which they will occur.

### Data model
#### lookup tasks
- user
- "not before" - the request should not happen before this time
- expires - time after which this task is invalid (presumably superseded by the next hourly task)

## Key lookup service
Performs the scheduled key lookups and saves the results.

### Data model
#### keys
- user
- lookup task
- lookup time
- lookup metadata (IP, phone) - in case we perform lookups from different vantage points
- key value

## Change detection service

Looks at the saved keys and checks if any of the ones since the previous run are different from their prior version. If a difference is detected, stores a record of it as a way of queuing a notification.

### Data model
#### key changes

- user
- first key occurrence (foreign key)
- last key occurrence (foreign key)
- new key first occurrence (foreign key)
- detection time

## Notification service

For each as-yet unprocessed key change, sends a notification to the affected user. Saves this fact to keep a record.

### Data model
#### notifications

- user
- key change
- notification type = email
- notification email (foreign key)
- notification time
- notes (alternately: errors) - any delivery problems

# Data

Data from all the services will be stored in separate tables of a single common relational database. This choice is made because of the reliability, interoperability, and operational simplicity of this option.

The data model (see above) was designed to favor appending records rather than updating or overwriting existing ones. This approach is preferred for maintainability, ease of synchronization, debugging, and recovering from errors.

Additionally, the tables identified above have a `status` field representing the current state of the record (i.e., whether or not it has been processed or not).

# Service architecture

The current plan is for each of the services described above (except the Unsubscribe service, an always-on web server) to work as stand-alone command-line applications, scheduled through a cron job.

Later, we may look at moving some of the services into AWS Lambda. #devopshipsters

# Language & technology choices

Because the unofficial [Signal CLI](https://github.com/AsamK/signal-cli), which we expect to be drawing on, and the official [library for communicating with Signal](https://github.com/WhisperSystems/libsignal-service-java) are written in Java, we plan to use a compatible language.

# External services

- The Signal server, obviously
- We will probably utilize an external service (e.g., Mailgun) for sending emails.

# Security considerations

_For our threat model, please see the project specs._

## Communication security

- We'll communicate with our external dependencies over HTTPS.
- Communications with users over Signal are protected by the underlying protocol.
- The Unsubscribe web server will use HTTPS.

## Authentication

- We will rely on Signal's phone number verification to ensure users really possess the phone number in question.
- We will verify users' email (when needed) by having them click on a link in a message.

## Data security

User data will not be publicly accessible. All information in the database will be stored in plaintext, because it is not considered especially sensitive (e.g., phone numbers and associated are publicly available from Signal).

# Other considerations

## Load on Signal servers

The operation of our monitoring service will place some extra load on Signal's servers. However, we don't believe this load will be disproportionate or burdensome. For each registered phone number, we will only make one request per hour, many fewer that the phone issues as part of normal operations (receiving and sending messages). Furthermore, we will spread out the requests, as discussed above.

