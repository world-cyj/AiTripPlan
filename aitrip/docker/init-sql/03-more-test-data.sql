-- AiTrip 测试数据 v3
USE aitrip;

INSERT IGNORE INTO tb_attraction
  (id,name,city,type_id,description,price,score,address,open_hours,longitude,latitude,deleted)
VALUES
(1001,'bingmayong','xian',1,'shijie dibaodaiqiji',120.00,4.9,'shanxisheng xianshi lintonqu','08:30-17:00',109.2784,34.3844,0),
(1006,'gugong','beijing',1,'mingqing liangdai huangjia gongshu',60.00,4.9,'beijing dongchengqu jingshan qianjie','08:30-17:00',116.3974,39.9163,0),
(1009,'badaling changcheng','beijing',1,'wanli changcheng zuijudaibiaxingduanluo',40.00,4.8,'beijing yanqingqu G6','07:30-17:00',116.0133,40.3574,0),
(1010,'xihu','hangzhou',3,'yuba xihu bi xizi lantaizong zong xiangyi',0.00,4.9,'zhejiang hangzhou xihugu longjinglu','全天',120.1551,30.2461,0),
(1013,'jiuzhaigou','chengdu',3,'shijie ziran yichan tonghua shijie',220.00,4.9,'sichuan abazhou jiuzhaigou xian','08:00-17:00',103.9186,33.2200,0),
(1014,'waitan','shanghai',1,'shanghai de xiangzheng wanguo jianzhu bolanhun',0.00,4.7,'shanghai huangpuqu zhongshan dong yilu','全天',121.4852,31.2400,0),
(1018,'zhuozhengyuan','suzhou',3,'zhongguo sida mingyuan jiangnan gudian yuanlin',90.00,4.8,'jiangsu suzhou gusugu dongbei jie','07:30-17:30',120.6313,31.3239,0),
(1020,'hongyaidong','chongqing',1,'chongqing wanghong dibiao diaojiaoloujunzhu',0.00,4.5,'chongqing yuzhongqu jialing jiang binjian lu','10:00-23:00',106.5718,29.5650,0);

INSERT IGNORE INTO tb_voucher
  (id,attraction_id,title,sub_title,rules,pay_value,actual_value)
VALUES
(1001,1001,'Special Ticket A','Limited 100','One per person',100.00,120.00),
(1002,1006,'Special Ticket B','Holiday limited','One per person',50.00,60.00),
(1003,1013,'Special Ticket C','Peak season','One per person',180.00,220.00),
(1004,1009,'Special Ticket D','Weekend limited','One per person',30.00,40.00),
(1005,1018,'Special Ticket E','Bundle deal','One per person',120.00,150.00);

INSERT IGNORE INTO tb_seckill_voucher (voucher_id,stock,begin_time,end_time)
VALUES
(1001,100,NOW(),DATE_ADD(NOW(),INTERVAL 30 DAY)),
(1002,50,NOW(),DATE_ADD(NOW(),INTERVAL 30 DAY)),
(1003,30,NOW(),DATE_ADD(NOW(),INTERVAL 30 DAY)),
(1004,80,NOW(),DATE_ADD(NOW(),INTERVAL 30 DAY)),
(1005,40,NOW(),DATE_ADD(NOW(),INTERVAL 30 DAY));

SELECT CONCAT('attraction count: ',COUNT(*)) AS r FROM tb_attraction WHERE deleted=0;
SELECT CONCAT('voucher count: ',COUNT(*)) AS r FROM tb_voucher;
SELECT CONCAT('seckill count: ',COUNT(*)) AS r FROM tb_seckill_voucher;
