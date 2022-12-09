#!/bin/bash

declare -A types
types=( ["commander"]="Other" ["core"]="Core")
declare -A rarities
rarities=( ["mythic"]="M" ["rare"]="R" ["uncommon"]="U" ["common"]="C" ["rare"]="R" )

setname=$1
wget -q -O /tmp/setinfo.json "https://api.scryfall.com/sets/$setname?format=json"
wget "https://api.scryfall.com/cards/search?order=set&unique=art&q=set%3D$setname" -q -O /tmp/set.json

cat /tmp/set.json | jq ".data | .[].collector_number" | sed "s/\"//g" > /tmp/cardidlist

hasmore=`cat /tmp/set.json | jq ".has_more" | sed "s/\"//g"`
nextpage=`cat /tmp/set.json | jq ".next_page" | sed "s/\"//g"`
while [ $hasmore ] ; do
	sleep 0.01
	wget "$nextpage" -q -O /tmp/next.json
	cat /tmp/next.json | jq ".data | .[].collector_number" | sed "s/\"//g" >> /tmp/cardidlist
	hasmore=`cat /tmp/next.json | jq ".has_more" | sed "s/\"//g"`
	nextpage=`cat /tmp/next.json | jq ".next_page" | sed "s/\"//g"`
done


cardIDs=($(cat /tmp/cardidlist))

sleep 0.1

echo "[metadata]"
code=`echo "$1" | awk '{print toupper($0)}'`
dateReleased=`cat /tmp/setinfo.json | jq ".released_at" | sed "s/\"//g"`
name=`cat /tmp/setinfo.json | jq ".name" | sed "s/\"//g"`
echo "Code=$code"
echo "Date=$dateReleased"
echo "Name=$name"
echo "Code2=$code"
echo "Type=Other"
echo ""
echo "[cards]"
for i in "${cardIDs[@]}"
do
	sleep 0.01
	wget -q -O /tmp/$i.json "https://api.scryfall.com/cards/$setname/$i?format=json"
	name=`cat /tmp/$i.json | jq .name | sed "s/\"//g"`
	basiclandtype=`cat /tmp/$i.json | jq .type_line | sed "s/\"//g" | grep "Basic Land" | wc -l`
	if [ $basiclandtype -gt 0 ]; then
	    rarity="L"
	else
	    rarityString=`cat /tmp/$i.json | jq .rarity | sed "s/\"//g"`
	    rarity=${rarities[$rarityString]}
	fi
	cnumber=`cat /tmp/$i.json | jq .collector_number | sed "s/\"//g"`
	echo "$cnumber $rarity $name"


done
