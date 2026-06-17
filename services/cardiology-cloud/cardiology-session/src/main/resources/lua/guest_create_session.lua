-- 游客创建会话：原子校验 session 数量上限并写入 Redis。
--
-- KEYS[1] indexKey   cardiology:guest:chat:{uid}:index
-- KEYS[2] metaKey    cardiology:guest:chat:{uid}:s:{sessionId}
-- KEYS[3] msgsKey    cardiology:guest:chat:{uid}:s:{sessionId}:msgs
--
-- ARGV[1] sessionId
-- ARGV[2] ttlSeconds
-- ARGV[3] maxSessions
-- ARGV[4] nowEpochMilli
-- ARGV[5] title
-- ARGV[6] uid
--
-- 返回值：1=成功，-1=会话数已满，-2=会话已存在

local indexKey = KEYS[1]
local metaKey = KEYS[2]
local msgsKey = KEYS[3]
local sessionId = ARGV[1]
local ttl = tonumber(ARGV[2])
local maxSessions = tonumber(ARGV[3])
local nowTs = ARGV[4]
local title = ARGV[5]
local uid = ARGV[6]

if redis.call('EXISTS', metaKey) == 1 then
    return -2
end

local count = redis.call('ZCARD', indexKey)
if count >= maxSessions then
    return -1
end

redis.call('ZADD', indexKey, nowTs, sessionId)
redis.call('HSET', metaKey,
    'sessionId', sessionId,
    'uid', uid,
    'title', title,
    'preview', '',
    'messageCount', '0',
    'userMessageCount', '0',
    'status', 'active',
    'pinned', 'false',
    'createdAt', nowTs,
    'updatedAt', nowTs
)

redis.call('EXPIRE', indexKey, ttl)
redis.call('EXPIRE', metaKey, ttl)
redis.call('EXPIRE', msgsKey, ttl)

return 1
