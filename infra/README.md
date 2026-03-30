# infra
이번 프로젝트에서는 쿠버네티스 기반으로 MSA 서버와 저장소 인프라를 모두 Minikube 환경에 구축할 예정이다.  

아래 블로그 목록은 쿠버네티스를 학습하며 직접 작성한 것으로, 쿠버네티스 환경을 구축할 때 참고하면 좋다.  
* [Minikube 환경 구축 블로그](https://little-pecorino-c28.notion.site/Pod-31c82094ef0a8094a26fce4881b5e805)


### 디렉터리 구조
```
ecommerce-data-platform/
└── infra/                  
    ├── platforms/          # 공통 인프라 (Stateful 기반)
    │   ├── clickhouse/
    │   ├── kafka/
    │   ├── ksqldb/
    │   └── postgres/
    └── apps/               # 비즈니스 마이크로서비스 (Stateless 기반)
        ├── settlement-service.yaml
        └── product-service.yaml
```

위와 같은 구조로 구성한 이유는 아래와 같다.
* 생명주기(Lifecycle) 분리 : platforms/ 하위의 Kafka와 같은 저장소 인프라는 한 번 배포하면 삭제하거나 재배포할 일이 거의 없다. 반면 apps/ 하위의 서비스들은 코드 변경이 발생할 때마다 kubectl apply가 자주 수행된다.
* 리소스 성격 차이 : platforms/ 하위는 주로 StatefulSet, PersistentVolume, Secret과 같은 상태 기반 리소스를 다루며, apps/ 하위는 Deployment, HPA(오토스케일링), Ingress와 같은 애플리케이션 실행 및 트래픽 관리 리소스를 중심으로 구성된다.
* 가독성 : infra/ 디렉터리에 모든 매니페스트를 한 곳에 모아두면 파일 수가 많아질수록 관리가 어려워진다. 서비스 또는 인프라 유형별로 디렉터리를 분리하면 특정 서비스의 설정만 빠르게 찾아 수정하기 쉽다.


### 인프라 구축 명령어
* [Kafka 구축 블로그](https://little-pecorino-c28.notion.site/Helm-Chart-Kafka-32282094ef0a80c88123dd6e1aa1d505#32282094ef0a80b68543cb482c87a3ec)

```shell
# 로컬 개발용 인프라 구축
docker-compose -f infra/docker-compose.dev.yml up -d


# Kafka, Kafka UI, ksqlDB
helm repo add strimzi https://strimzi.io/charts/
helm repo update
kubectl create namespace kafka
helm install strimzi-operator strimzi/strimzi-kafka-operator -n kafka

kubectl apply -f infra/platforms/kafka/kafka-cluster.yaml -n kafka
kubectl apply -f infra/platforms/kafka/kafka-ui.yaml -n kafka
kubectl apply -f infra/platforms/ksqldb/ksqldb.yaml -n kafka
# 호스트에서 Kafka UI 접근 시 연결
kubectl port-forward svc/kafka-ui -n kafka 8090:80


# postgres
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
kubectl create namespace postgres

helm install postgres bitnami/postgresql -f infra/platforms/postgres/postgres-values.yaml -n postgres
# 호스트에서 postgres 접근 시 연결
kubectl port-forward svc/postgres-postgresql -n postgres 5432:5432


# clickhouse
kubectl apply -f infra/platforms/clickhouse/clickhouse.yaml
# 호스트에서 clickhouse 접근 시 연결
kubectl port-forward svc/clickhouse-svc -n clickhouse 8123:8123


# settlement-service 실행
kubectl apply -f infra/apps/settlement-service.yaml
# analytics-service 실행
# 최초 실행 시 analytics-service 프로젝트 resources/db 경로에 스키마 파일들을 적용해야 함
kubectl apply -f infra/apps/analytics-service.yaml
```


### Kafka 토픽 생성

아래는 Kafka 클러스터가 구축되고 난 후 파티션 수를 지정하기 위해 직접 토픽 생성 명령어를 실행하였다.

```shell
# 로컬에서 토픽 생성
docker exec -it kafka1 kafka-topics --create --topic order-created --bootstrap-server kafka1:9091 --partitions 3 --replication-factor 3
docker exec -it kafka1 kafka-topics --create --topic order-cancelled --bootstrap-server kafka1:9091 --partitions 3 --replication-factor 3
docker exec -it kafka1 kafka-topics --create --topic payment-confirmed --bootstrap-server kafka1:9091 --partitions 3 --replication-factor 3
docker exec -it kafka1 kafka-topics --create --topic payment-cancelled --bootstrap-server kafka1:9091 --partitions 3 --replication-factor 3
docker exec -it kafka1 kafka-topics --create --topic user_registered --bootstrap-server kafka1:9091 --partitions 3 --replication-factor 3
docker exec -it kafka1 kafka-topics --create --topic inventory_changed --bootstrap-server kafka1:9091 --partitions 3 --replication-factor 3
docker exec -it kafka1 kafka-topics --create --topic page_viewed --bootstrap-server kafka1:9091 --partitions 3 --replication-factor 3

# 쿠버네티스에서 토픽 생성
kubectl exec -it my-cluster-kafka-nodes-0 -n kafka -- /opt/kafka/bin/kafka-topics.sh --create --topic order-created --bootstrap-server my-cluster-kafka-bootstrap:9092 --partitions 3 --replication-factor 1
kubectl exec -it my-cluster-kafka-nodes-0 -n kafka -- /opt/kafka/bin/kafka-topics.sh --create --topic order-cancelled --bootstrap-server my-cluster-kafka-bootstrap:9092 --partitions 3 --replication-factor 1
kubectl exec -it my-cluster-kafka-nodes-0 -n kafka -- /opt/kafka/bin/kafka-topics.sh --create --topic payment-confirmed --bootstrap-server my-cluster-kafka-bootstrap:9092 --partitions 3 --replication-factor 1
kubectl exec -it my-cluster-kafka-nodes-0 -n kafka -- /opt/kafka/bin/kafka-topics.sh --create --topic payment-cancelled --bootstrap-server my-cluster-kafka-bootstrap:9092 --partitions 3 --replication-factor 1
kubectl exec -it my-cluster-kafka-nodes-0 -n kafka -- /opt/kafka/bin/kafka-topics.sh --create --topic user_registered --bootstrap-server my-cluster-kafka-bootstrap:9092 --partitions 3 --replication-factor 1
kubectl exec -it my-cluster-kafka-nodes-0 -n kafka -- /opt/kafka/bin/kafka-topics.sh --create --topic inventory_changed --bootstrap-server my-cluster-kafka-bootstrap:9092 --partitions 3 --replication-factor 1
kubectl exec -it my-cluster-kafka-nodes-0 -n kafka -- /opt/kafka/bin/kafka-topics.sh --create --topic page_viewed --bootstrap-server my-cluster-kafka-bootstrap:9092 --partitions 3 --replication-factor 1
```