部署流程：
1 安装jdk , 版本需 >= 1.8
2 安装maven，并设置好环境变量，在cmd里输入mvn -version验证是否安装正确
3 在cmd里进入inchain-core目录，运行install.bat
4 在cmd里进入inchain(父级目录)，运行命令 mvn eclipse:eclipse
5 导入项目到eclipse里
6 设置M2_REPO

注：3-4步也可运行deploy-eclipse.bat代替

更多资料请到 http://bbs.inchain.org/forumdisplay.php?fid=7 查阅