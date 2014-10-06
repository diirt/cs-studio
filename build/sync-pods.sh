#!/bin/bash

# sync_dir dir src_location dest_location
# e.g. sync_dir util $HGDIR/src/main/java/org/epics ../core/plugins/org.epics.util/src/org/epics/
function sync_dir {
    find $2/$1 -type f -exec grep -qI '' {} ';' -exec perl -pi -e 's/\r\n/\n/g' {} '+'
    rsync -r --delete $2/$1 $3
    git add $3/$1
}

# Go into build directory
BASEDIR=$(dirname $0)
cd $BASEDIR

HGDIR=diirt_tmp
rm -rf $HGDIR
echo pods repo
git clone git@github.com:diirt/pods.git $HGDIR

echo Synching web-pods-common
sync_dir web $HGDIR/web-pods-common/src/main/java/org/diirt ../core/plugins/org.diirt.web.pods.common/src/org/diirt/
git commit --author="Gabriele Carcassi <gabriele.carcassi@gmail.com>" -m "org.diirt.pods.web.common: update to current SNAPSHOT" ../core/plugins/org.diirt.web.pods.common
echo Done web-pods-common

echo Synching web-pods-datasource
sync_dir datasource $HGDIR/web-pods-datasource/src/main/java/org/diirt ../core/plugins/org.diirt.datasource.pods.web/src/org/diirt/
git commit --author="Gabriele Carcassi <gabriele.carcassi@gmail.com>" -m "org.diirt.datasource.pods.web: update to current SNAPSHOT" ../core/plugins/org.diirt.datasource.pods.web
echo Done web-pods-datasource

rm -rf $HGDIR

