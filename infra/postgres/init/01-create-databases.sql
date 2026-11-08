-- 컨테이너 최초 생성 시 1회 실행. 앱용 classmento DB는 POSTGRES_DB로 이미 만들어진다.
CREATE DATABASE classmento_test OWNER classmento;

\connect classmento
CREATE EXTENSION IF NOT EXISTS vector;

\connect classmento_test
CREATE EXTENSION IF NOT EXISTS vector;
