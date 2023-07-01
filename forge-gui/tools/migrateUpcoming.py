import os
import subprocess

cardsfolder = os.path.join(os.path.dirname(os.getcwd()), 'res', 'cardsfolder')
upcoming = os.path.join(cardsfolder, 'upcoming')

for dirName, subdirList, fileList in os.walk(upcoming):
	for filename in fileList:
		if filename.startswith("."):
			continue
		curLocation = os.path.join(upcoming, filename)
		newFile = os.path.join(cardsfolder, filename[0], filename)

		if os.path.exists(newFile):
			subprocess.call('git rm %s' % (curLocation), shell=True)
		else:
			subprocess.call('git mv %s %s' % (curLocation, newFile), shell=True)