

cd /cygdrive/d/sources/_github/forks/jmx_exporter

for toggle in false true
do

  java -DBULK_FETCH=$toggle -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=5555 -jar jmx_prometheus_httpserver/target/jmx_prometheus_httpserver-0.2.1-SNAPSHOT-jar-with-dependencies.jar 5556 example_configs/httpserver_sample_config.yml &
  pid=$!
  echo "started agent with pid: $pid for toggle: $toggle"

  >$toggle.txt
  count=5
  while (( --count >= 0 )); 
  do 
    curl "http://localhost:5556/metrics" 2>&1 /dev/null|grep jmx_scrape_duration_seconds|grep -v "#" |tee -a $toggle.txt
  done
  
  $(kill $pid) 2>&1 /dev/null
  $(jobs) 2>&1 /dev/null

done

#compare samples
old=$(awk '{sum+=$NF}END{print sum;}' false.txt)
new=$(awk '{sum+=$NF}END{print sum;}' true.txt)
echo "diff $old -> $new"

