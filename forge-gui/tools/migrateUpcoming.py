import os
import subprocess

cardsfolder = os.path.join(os.path.dirname(os.getcwd()), 'res', 'cardsfolder')
upcoming = os.path.join(cardsfolder, 'upcoming')

for dirName, subdirList, fileList in os.walk(upcoming):
	for file in fileList:
		curLocation = os.path.join(upcoming, file)
		newFile = os.path.join(cardsfolder, file[0], file)
		subprocess.call('svn rename %s %s' % (curLocation, newFile), shell=True)