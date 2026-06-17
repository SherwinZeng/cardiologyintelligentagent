-- 游客发送 user 消息：原子校验 30 条上限并写入 Redis LIST。
--
-- KEYS[1] indexKey
-- KEYS[2] metaKey
-- KEYS[3] msgsKey
--
-- ARGV[1] ttlSeconds
-- ARGV[2] maxUserMessages
-- ARGV[3] nowEpochMilli
-- ARGV[4] sessionId
-- ARGV[5] userMsgJson
--
-- 返回值：1=成功，-2=问题数已满，-3=会话不存在

local indexKey = KEYS[1]
local metaKey = KEYS[2]
local msgsKey = KEYS[3]
local ttl = tonumber(ARGV[1])
local maxUserMessages = tonumber(ARGV[2])
local nowTs = ARGV[3]
local sessionId = ARGV[4]
local userMsgJson = ARGV[5]

if redis.call('EXISTS', metaKey) == 0 then
    return -3
end

local userCount = tonumber(redis.call('HGET', metaKey, 'userMessageCount') or '0')
if userCount >= maxUserMessages then
    return -2
end

redis.call('HINCRBY', metaKey, 'userMessageCount', 1)
redis.call('HINCRBY', metaKey, 'messageCount', 1)
redis.call('LPUSH', msgsKey, userMsgJson)
redis.call('HSET', metaKey, 'updatedAt', nowTs)
redis.call('ZADD', indexKey, nowTs, sessionId)

redis.call('EXPIRE', indexKey, ttl)
redis.call('EXPIRE', metaKey, ttl)
redis.call('EXPIRE', msgsKey, ttl)

return 1
