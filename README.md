# User Management

A small full-stack application for managing users: a Spring Boot REST API and a single-screen Angular
client. A user has a name, which can be changed, and an email address, which is fixed at creation.

## Status

Planning. The work is laid out in [ROADMAP.md](ROADMAP.md) and [plan/](plan/). The build and run
commands are added with the code.

## Stack

- Java 21, Spring Boot 4.2, Maven wrapper
- Angular 22, Node.js 24

## Database

H2 in memory. Nothing to install, and Hibernate creates the schema at startup. The data is lost when
the application stops.
