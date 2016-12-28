echo into core dir ...............
cd inchain-core

echo install core jar to maven ...............
call install.bat

cd ../

mvn clean install -Dmaven.test.skip=true
mvn eclipse:eclipse