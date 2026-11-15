// 本地 mock 后端：只为安卓端联调/演示，不含业务逻辑，不替代真实后端。
// 用法：node server.mjs   （默认监听 0.0.0.0:8000）
// 模拟器访问：http://10.0.2.2:8000
// 预置账号：demo / 123456
import http from 'node:http'
import { randomUUID } from 'node:crypto'

const PORT = Number(process.env.PORT || 8000)
const users = new Map() // username -> { id, username, nickname, role, password }
let nextUserId = 1

function addUser(username, password, nickname) {
  const user = {
    id: nextUserId++,
    username,
    nickname: nickname || username,
    role: 'user',
    password,
  }
  users.set(username, user)
  return user
}

addUser('demo', '123456', '演示用户')

function json(res, status, payload, extraHeaders = {}) {
  const body = JSON.stringify(payload)
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'X-Trace-Id': randomUUID(),
    ...extraHeaders,
  })
  res.end(body)
}

function ok(res, data, status = 200) {
  json(res, status, { code: 0, message: 'ok', data })
}

function fail(res, status, code, message) {
  json(res, status, { code, message })
}

function readBody(req) {
  return new Promise((resolve) => {
    let raw = ''
    req.on('data', (chunk) => (raw += chunk))
    req.on('end', () => {
      try {
        resolve(raw ? JSON.parse(raw) : {})
      } catch {
        resolve({})
      }
    })
  })
}

// 只认签发过的 token：进程重启后这里清空，旧 token 立刻变成 40102，
// 跟真实后端「换 JWT_SECRET 后旧 token 全部失效」的已知坑一致，方便演示 401 统一登出。
const issuedTokens = new Set()

function currentUser(req) {
  const auth = req.headers['authorization'] || ''
  if (!auth.startsWith('Bearer ')) return null
  const token = auth.slice(7)
  if (!issuedTokens.has(token)) return null
  return users.get(token.slice('mock-token-'.length)) || null
}

const homeData = {
  banners: [
    { id: 1, title: '云栖竹径', subtitle: '竹影婆娑，一步一景', imageUrl: '' },
    { id: 2, title: '九溪烟树', subtitle: '溪水潺潺，秋色正好', imageUrl: '' },
    { id: 3, title: '灵隐古刹', subtitle: '千年古寺，钟声悠远', imageUrl: '' },
  ],
  quickEntries: [
    { id: 1, title: 'AI 问答', target: 'chat' },
    { id: 2, title: '景点', target: 'poi' },
    { id: 3, title: '路线', target: 'route' },
    { id: 4, title: '失物招领', target: 'lostfound' },
  ],
  hotPois: [
    { id: 1, name: '灵隐寺', category: '人文古迹', location: '西湖区法云弄1号', description: '始建于东晋，江南著名古刹。' },
    { id: 2, name: '九溪十八涧', category: '自然风光', location: '西湖区九溪路', description: '溪流蜿蜒，适合徒步。' },
    { id: 3, name: '雷峰塔', category: '人文古迹', location: '南山路15号', description: '夕照雷峰，西湖十景之一。' },
  ],
  recommendedRoutes: [
    { id: 1, name: '西湖经典一日游', description: '断桥 → 白堤 → 孤山 → 岳庙', duration: '约 6 小时', distance: '7.5km' },
    { id: 2, name: '九溪徒步线', description: '九溪入口 → 溪中溪 → 茶园', duration: '约 3 小时', distance: '4.2km' },
  ],
}

const streamingSessions = new Map() // sessionId -> { aborted }

const CANNED_ANSWER =
  '灵隐寺始建于东晋咸和元年（公元326年），是杭州最早的名刹之一。' +
  '相传印度僧人慧理来到这里，见山峰奇秀，以为“仙灵所隐”，便在此建寺，取名灵隐。' +
  '寺内的大雄宝殿、飞来峰造像都值得细看；建议早上前往，人少且光线好。'

// 第二段只为把流式时长拉到 10s 以上，方便演示「逐字出现」和中途「停止」
const CANNED_ANSWER_MORE =
  '如果要安排半天行程，可以按「飞来峰造像 → 灵隐寺 → 永福寺 → 韬光寺」的顺序走，' +
  '全程约 3 小时，台阶较多，建议穿轻便的鞋。' +
  '门票需要先买飞来峰景区票，再单独购买灵隐寺香花券，两处排队高峰通常在上午十点之后。' +
  '寺内提供素面，中午可以在斋堂简单用餐；返程可沿九溪十八涧步行下山，溪水清凉，夏天尤其舒服。'

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms))
}

async function handleChatStream(req, res) {
  const body = await readBody(req)
  const message = body.message || (Array.isArray(body.messages) ? body.messages.at(-1)?.content : '')
  if (!message) {
    json(res, 400, { error: '请提供 message (string) 或 messages (array) 参数' })
    return
  }

  const sessionId = randomUUID()
  streamingSessions.set(sessionId, { aborted: false })

  res.writeHead(200, {
    'Content-Type': 'text/event-stream; charset=utf-8',
    'Cache-Control': 'no-cache',
    Connection: 'keep-alive',
    'X-Session-Id': sessionId,
    'X-Trace-Id': randomUUID(),
  })

  let seq = 0
  const answer = `关于「${message.trim()}」：${CANNED_ANSWER}${CANNED_ANSWER_MORE}`
  const chunks = answer.match(/[\s\S]{1,3}/g) || []

  for (const chunk of chunks) {
    if (streamingSessions.get(sessionId)?.aborted) break
    res.write(`event: token\nid: ${seq++}\ndata: ${JSON.stringify({ content: chunk })}\n\n`)
    await sleep(90)
  }

  if (streamingSessions.get(sessionId)?.aborted) {
    res.write(`event: error\nid: ${seq++}\ndata: ${JSON.stringify({ message: '已按用户请求停止生成' })}\n\n`)
    res.write(`event: done\nid: ${seq++}\ndata: {}\n\n`)
  } else {
    res.write(`event: done\nid: ${seq++}\ndata: ${JSON.stringify({ fullContent: answer })}\n\n`)
  }
  streamingSessions.delete(sessionId)
  res.end()
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`)
  const path = url.pathname
  const method = req.method

  if (method === 'OPTIONS') {
    res.writeHead(204, { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Headers': '*' })
    return res.end()
  }

  if (path === '/health' && method === 'GET') {
    return ok(res, { status: 'ok', mode: 'mock' })
  }

  if (path === '/api/v1/auth/register' && method === 'POST') {
    const body = await readBody(req)
    if (!body.username || !body.password || body.password.length < 6) {
      return fail(res, 400, 40001, '用户名不能为空，密码至少 6 位')
    }
    if (users.has(body.username)) return fail(res, 409, 40001, '用户名已存在')
    const user = addUser(body.username, body.password, body.nickname)
    return ok(res, { id: user.id }, 201)
  }

  if (path === '/api/v1/auth/login' && method === 'POST') {
    const body = await readBody(req)
    const user = users.get(body.username)
    if (!user || user.password !== body.password) {
      return json(res, 401, { code: 40101, message: '用户名或密码错误' })
    }
    const token = `mock-token-${user.username}`
    issuedTokens.add(token)
    return ok(res, {
      user: { id: user.id, username: user.username, nickname: user.nickname, role: user.role },
      token,
    })
  }

  if (path === '/api/v1/auth/me' && method === 'GET') {
    const user = currentUser(req)
    if (!user) return json(res, 401, { code: 40102, message: 'Token 无效或已过期' })
    return ok(res, { id: user.id, username: user.username, nickname: user.nickname, role: user.role })
  }

  if (path === '/api/v1/home' && method === 'GET') {
    // 真实后端的首页是需要登录的，这里同样校验 Bearer token
    if (!currentUser(req)) return json(res, 401, { code: 40102, message: 'Token 无效或已过期' })
    return ok(res, homeData)
  }

  if (path === '/api/v1/chat/stream' && method === 'POST') {
    return handleChatStream(req, res)
  }

  if (path === '/api/v1/chat/stop' && method === 'POST') {
    const body = await readBody(req)
    if (body.sessionId && streamingSessions.has(body.sessionId)) {
      streamingSessions.get(body.sessionId).aborted = true
    }
    return ok(res, { sessionId: body.sessionId || '', stopped: true })
  }

  if (path === '/api/v1/poi' && method === 'GET') {
    return ok(res, homeData.hotPois.map((p) => ({ ...p, status: 'online' })))
  }

  fail(res, 404, 40401, `mock 未实现该接口：${method} ${path}`)
})

server.listen(PORT, '0.0.0.0', () => {
  console.log(`mock 后端已启动: http://localhost:${PORT}  (模拟器用 http://10.0.2.2:${PORT})`)
  console.log('预置账号: demo / 123456')
})
