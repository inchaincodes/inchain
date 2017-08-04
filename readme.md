概述：  
在网络结构上，印链采用P2P网络结构，使用TCP连接自定义消息结构通讯。

节点发现
---

节点发现一般有三种：  
1.源码中写死几个种子节点服务器地址，通过种子节点获取更多节点地址  
2.通过DNS获取种子节点服务器地址，通过种子节点获取更多节点地址  
3.通过HTTP请求获取子节点服务器地址，通过种子节点获取更多节点地址  

Inchain目前主要采用第二种节点发现方式。

共用结构
---

绝大多数整数都都使用little endian编码，只有IP地址或端口号使用big endian编码。

varint：变长整数，可以根据表达的值进行编码以节省空间。

|值|长度|格式|
|---|---|---|
|< 0xfd|1|uint8|
|<= 0xffff|3|0xfd + uint16|
|<= 0xffffffff|5|0xfe + uint32|
|> 0xffffffff|9|0xff + uint64|

varstr：变长字符串，由一个变长整数后接字符串构成。字符串采用UTF8编码。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|?|length|varint|字符串的长度，以字节为单位|
|length|string|uint8[length]|字符串本身|

array：数组，由一个变长整数后接元素序列构成。



1.PeerAddress
> 印链封装的节点信息结构,兼容IPv6与IPv4地址。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|8| Timestamp |int64|当前时间|
|4|Service Version|uint32|服务版本|
|16|IP Address|byte[16]|IPv6地址|
|2|Port|uint16|端口|

> Peer Address同时兼容IPv6与IPv4地址。  
> 当地址为IPv4时，地址存储在buf[12]开始的最后4字节处。同时设置buf[10]和buf[11]为0xFF。

2.InventoryItem

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|1|type|byte|清单类型|
|32|hash|SHA256|哈希|

> 已定义的type值
> 
> |值|类型|说明|
> |---|---|---|
> |0x01|Transaction|交易|
> |0x02|Block|区块|
> |0x03|NewBlock|新区块|
> |0x04|Consensus|共识|

3.TransactionHeader

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|4|type|int32|交易类型|
|4|version|uint32|交易版本|
|?|input tx count|VarInt|输入交易数|
|?|input|input[]|输入交易|
|?|output tx count|VarInt|输出交易数|
|?|output|output[]|输出交易|
|8|time|int64|时间戳|
|8|lock time|int64|锁定时间，小于0永久锁定，大于等于0为锁定的时间或者区块高度|
|?|remark|varstr|备注|
|?|sign script|varstr|签名脚本|

> remark不能超过100字节。
> lockTime 小于0永久锁定，大于等于0为锁定的时间或者区块高度


4.BlockHeader

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|4|version|uint32|区块版本|
|32|prev Hash|SHA256|上一个区块Hash|
|32|markle Hash|SHA256|markle Hash|
|4|time|Timespan|时间戳|
|4|hight|uint32|高度|
|4|period count|uint32|该时段共识人数|
|4|time period|int32|时段，一轮共识中的第几个时间段|
|4|period start time|uint32|本轮开始的时间点|
|4|script length|uint32|验证脚本长度|
|?|script|byte[?]|验证脚本|
|4|tx count|uint32|交易数量|
|?|txHash[]|sha256[?]|交易签名表|

5.Sign

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|4|sign count|int32|签名长度|
|?|sign|byte[?]|签名

协议
---

### 公共结构
1.包头
> 所有对等节点通讯消息的包头。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|4|MagicNo|uint32|魔法值|
|12|Command|char[12]|命令类型|
|4|Payload Length|uint32|消息长度|
|4|Checksum|uint32|校验和|
|?|Payload|uint8[?]|消息体|

已定义的MagicNo值

> |值|说明|
> |---|---|
> |0x05209A2A|正式网|
> |0x2581D888|测试网|
> |0x0001B6A0|单元测试网|

Command采用utf8编码，长度为12字节，多余部分用0填充。

Checksum是Payload两次SHA256散列后的前4个字节。

Payload根据不同的命令有不同的详细格式，见下文 【消息结构】。


### 消息结构
---
version

> TCP连接成功后，Client主动发送给Remote Peer。  
> 在双向握手成功之前，不会有其他消息发送。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|1|localServices|uint8|哪个网络服务|
|4|client version|uint32|客户端版本|
|8|local time|uint64|时间戳|
|30|local peer address|PeerAddress|本机地址|
|30|remote peer address|PeerAddress|请求者地址|
|4|UserAgent length|uint32|版本字符串长度|
|?|UserAgent|byte[?]|版本字符串|
|4|best block height|uint32|最适合高度|
|32|best block hash|char[32]|最适合高度块Hash字符串|
|8|nonce|int64|随机数|

> local Services 可选值：1 正式网络 2 测试网络

---
verack
> 收到Version消息后，抽取nonce，响应version命令。
> UserAgent为UTF-8编码字符串


|尺寸|字段|数据类型|说明|
|---|---|---|---|
|8|current time|int64|当前时间戳|
|8|nonce|int64|收到的VersionMessage中的nonce值|

---
ping
> 心跳协议，探测对等节点是否正常提供服务。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|8|nonce|int64|随机数|

---
pong
> 心跳协议，响应ping命令。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|8|nonce|int64|收到的Ping包中的nonce|

---
getaddr
> 从对等节点请求握手成功的服务节点地址。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|8|time|int64|当前时间戳|

---
addr
> 响应getaddr命令，返回本地握手成功的节点信息。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|4|size|int32|节点数|
|30*?|peer address table|PeerAddress[size]|服务节点信息|


---
inv
> 数据向量清单。最大Item长度不超过10000。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|4|size|int32| InventoryItem数量|
|33*?|invItem| InventoryItem[]|数据表|

---
getdatas
> 向对等节点发送下载数据的消息，包括区块和交易，一般用于回应inv命令。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|4|size|int32| InventoryItem数量|
|33*?|invItem| InventoryItem[]|数据表|

---
getblock

> 当节点启动时，发现本地区块比连接的对等节点区块高度小，则发送该消息下载同步区块信息。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|32|start hash|sha256|开始区块hash|
|32|stop hash|sha256|结束区块hash|

---
block

> 当对等节点收到getblock消息后，有能分享的数据，则使用block消息响应请求。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|?|block header|BlockHeader|block头|
|?*?|Transactions|tx[]|交易列表|

---
newblock
> 当节点产生新块时，可以发送该消息。也可以发送inv消息。
> inv类型中有一个是newblock，会以该消息回应getdata消息。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|?|block header|BlockHeader|block头|

---
notfound
> 当收到对等节点发送的获取数据消息（区块、交易等）后，没有对应的数据，则回应notfound。

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|32|hash|sha256|请求参数hash|

---
consensus
> 共识

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|4|protocol version|int32|协议版本|
|20|sender|hash160|消息发送人|
|4|height|uint32|高度|
|8|time|Timespane|时间戳|
|8|nonce|int64|随机数|
|?|content|varstr|内容|
|?*?| Signs| Sign[]|签名|


TX协议
---
> 注意：所有TX交易都以remark长度和remark内容结尾
> 
> |尺寸|字段|数据类型|说明|
> |---|---|---|---|
> |?|remark length|varint|remark长度|
> |?|remark|byte[?]|remark|


---


CommonlyTransaction

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|1|Type|byte|交易类型|
|4|version|uint32|版本号|
|4|time|uint32|时间|
|?|script length|varint|签名内容长度,可能为0|
|?|script|byte[?]|签名|


---

AntifakeCodeBindTransaction : CommonlyTransaction 

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|?|anti fake code|byte[]|产品防伪码|
|32|product tx |sha256|关联产品|
|8|nonce|int64|产品编号|

---

AntifakeCodeMakeTransaction : CommonlyTransaction 

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|4|has product|int32|是否关联产品 0:关联 1:不关联|
|32|product tx|sha256|关联产品，仅当hasProduct==1时存在|
|8|nonce|int64|产品编号|

---

AntifakeCodeVerifyTransaction : CommonlyTransaction 

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|?|antifakecode|byte[]|防伪码|
|?|longitude|double|验证时的位置信息|
|?|latitude|double|验证时的位置信息|

---

AntifakeTransferTransaction : CommonlyTransaction 

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|?|antifakecode|byte[]|防伪码|
|32|receiver|sha256|接收人|
|?|remark length|varint|remark长度|
|?|remark|byte[]|备注|

---

AssetsIssuedTransaction : CommonlyTransaction 

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|32|assets|sha256|资产Hash|
|32|receiver|sha256|接收人|
|8|amount|int64|数量|

---

AssetsRegisterTransaction : CommonlyTransaction 

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|?|name|varstr|资产名称|
|?|description|varstr|资产描述|
|?|code|varstr|资产代号|
|?|logo|varstr|资产Logo图片|

---

AssetsTransferTransaction : AssetsIssuedTransaction 

> 无附加参数

---

CertAccountRegisterTransaction

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|4|type|int32|交易类型|
|4|version|uint32|版本|
|4|time|uint32|时间|
|32|hash|sha256|账户信息|
|32|superhash|sha256|superHash160|
|4|level|int32|level|
|?|bodycontent length|varint|主体长度|
|?|bodyContent|byte[]|主体|
|?|pubkeys count|varint|账户公钥数|
|?*?|pubkeys|array[byte[]]|账户公钥组|
|?|transPubKeys count|int|交易公钥数|
|?\*?|transPubKeys|array[byte[]]|交易公钥|
|?|script|varstr|签名|

---

CertAccountRevokeTransaction

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|4|type|int32|交易类型|
|4|version|uint32|版本|
|4|time|uint32|时间|
|32|revoke hash|sha256|撤销信息|
|32|hash|sha256|账户信息|
|32|superhash|sha256|superHash160|
|4|level|int32|level|
|?|bodycontent length|varint|主体长度|
|?|bodyContent|byte[]|主体|
|?|pubkeys count|varint|账户公钥数|
|?*?|pubkeys|array[byte[]]|账户公钥组|
|?|transPubKeys count|int|交易公钥数|
|?\*?|transPubKeys|array[byte[]]|交易公钥|
|?|script|varstr|签名|


---

CertAccountUpdateTransaction : CertAccountRegisterTransaction

> 无扩展数据

---

CirculationTransaction: CommonlyTransaction 

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|?|antifakecode|varstr|防伪码|
|?|tag|varstr|标签|
|?|content|varstr|内容|


---

CreditTransaction : CommonlyTransaction 

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|32|owner Hash160|SHA256|信用获得者|
|8|credit|int64|信用值|
|4|reason type|int32|变动原因|
|32|reason hash|sha256|交易签名，根据此交易做出的变动|

---

GeneralAntifakeTransaction 

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|4|type|int32|交易类型|
|4|version|uint32|版本|
|8|time|int64|时间|
|4|flags|int32|类型|
|32|product tx|sha256|关联产品|
|8|nonce|int64|商品编号|
|8|password|int64|商品验证密码|
|?|sign verification|varstr|商家签名信息|
|?|longitude|double|经度|
|?|longitude|latitude|纬度|
|?|script|varstr|签名|

---

ProductTransaction : CommonlyTransaction 

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|?|product|varstr|产品信息|

---

RegAliasTransaction : CommonlyTransaction 

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|?|alias length|varint|别名长度|
|?|alias|byte[?]|别名|


---

RegConsensusTransaction : CommonlyTransaction 

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|4|period start time|uint32|申请时的时段|
|?|packager|byte[?]|指定打包人|

---

RelevanceSubAccountTransaction : CommonlyTransaction 

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|?|relevanceHashs|byte[]|关联账户|
|?|alias|varstr|别名|
|?|content|varstr|描述|

---

RemConsensusTransaction : CommonlyTransaction 

|尺寸|字段|数据类型|说明|
|---|---|---|---|

---

RemoveSubAccountTransaction : CommonlyTransaction 

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|?|relevanceHashs|byte[]|关联账户|
|32|txhash|sha256|交易hash|

---

UnkonwTransaction : CommonlyTransaction 

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|?|content|byte[]|未知交易|

---

UpdateAliasTransaction : CommonlyTransaction 

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|?|alias|varstr|新别名|

---

ViolationTransaction : CommonlyTransaction 

|尺寸|字段|数据类型|说明|
|---|---|---|---|
|?|violationEvidence|ViolationEvidence|证据|