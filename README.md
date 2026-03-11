# ecommerce-data-platform
[이전 ecommerce-msa 프로젝트](https://github.com/gr1993/ecommerce-msa) 환경의 Kafka 이벤트를 기반으로 정산(Settlement) 및 실시간 분석(Analytics) 서비스를 구현한 데이터 플랫폼 프로젝트  

## 정산 서비스
정산(Settlement): 배치 처리(Spring Batch)와 데이터 대조(Reconciliation)가 핵심

## 분석 서비스
분석(Analytics): 실시간 스트림 처리(Kafka Streams/Flink)와 OLAP성 쿼리가 핵심