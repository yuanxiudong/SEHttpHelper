# HttpHelper说明：

>    1.HttpHelper是一个同步的HTTP请求接口,实现请求和响应模型，并对响应结果提供轻量级面向对象转换支持。
>    2.HttpHelper目前以及对Https提供支持。
>    3.目前上不支持文件上传。
>    4.目前不支持Proxy。

# HttpHelper使用帮助：

>    1.构建HttpRequest。可以配置HttpUrlConnection相关参数，Header的相关参数，以及请求参数。
>    2.调用HttpHelper进行Get/Post（请求方式：Http/Https）提交，提交过程会阻塞当前线程，请求完成以后会返回响应。
>    3.通过响应码识别当前请求是否成功或者失败。
