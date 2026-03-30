#!/bin/bash
# scripts/init-kafka-topics.sh
# Kafka Topic 初始化脚本 — AiTrip
# 使用方式: bash scripts/init-kafka-topics.sh

set -e

KAFKA_CONTAINER="aitrip-kafka"
BROKER="localhost:9092"

echo "[INFO] 等待 Kafka 就绪..."
until docker exec $KAFKA_CONTAINER kafka-broker-api-versions --bootstrap-server $BROKER &>/dev/null; do
  echo "[INFO] Kafka 尚未就绪，等待 5 秒..."
  sleep 5
done
echo "[INFO] Kafka 已就绪，开始创建 Topic..."

create_topic() {
  local TOPIC=$1
  local PARTITIONS=$2
  local REPLICATION=$3
  docker exec $KAFKA_CONTAINER kafka-topics \
    --create --if-not-exists \
    --bootstrap-server $BROKER \
    --topic "$TOPIC" \
    --partitions $PARTITIONS \
    --replication-factor $REPLICATION
  echo "[OK] Topic created: $TOPIC (partitions=$PARTITIONS)"
}

# 秒杀消息（高吞吐，8分区）
create_topic "aitrip.ticket.seckill" 8 1

# 秒杀死信队列
create_topic "aitrip.ticket.seckill.dlq" 2 1

# 订单创建
create_topic "aitrip.order.create" 8 1

# 通知发送
create_topic "aitrip.notify.send" 4 1

# 通用死信队列
create_topic "aitrip.dlq" 2 1

echo ""
echo "[INFO] 所有 Topic 创建完成，当前 Topic 列表："
docker exec $KAFKA_CONTAINER kafka-topics \
  --list --bootstrap-server $BROKER
