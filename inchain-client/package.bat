echo goto core dir ···
cd ../inchain-core

echo install core ···
call install.bat

echo back client dir ···
cd ../inchain-client/

echo package ···
mvn clean package -Dmaven.test.skip=true