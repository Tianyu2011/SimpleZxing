# SimpleZxing

## demo的样式
 ![SimpleZxing](http://7xsehv.com1.z0.glb.clouddn.com/qr_san_simpleZxingdemo.png)

##修改的部分
* 删除Zxing项目中大部分不使用的代码
* 添加手动打开闪关灯的功能
* 添加从图库打开图片解析二维码的功能
* 全屏幕都可以扫描二维码（包含阴影部分）

##相对于Zxing的demo，删除的功能
* 删除了保存历史记录
* 删除了查询图书二维码及wifi二维码的功能
* 删除了分享功能及简化了返回的result的处理逻辑
* 删除了PreferencesActivity，不再利用其存储使用状态
* 去掉了ViewfinderView,自定义扫描界面

##关于jar包
* 是我利用比较新的(2016年3月份下载的)源码打成的jar包，3.2.2版本。

##关于项目
* 有啥错误和需要改进的地方，麻烦写下issues,一起学习。

##关于版权
google的开源项目肯定是他们的，我只是改改...希望不要介意