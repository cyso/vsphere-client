VERSION=$(git describe)

changelog:
	    git dch --debian-branch=master --snapshot --auto
		 
release:
	    git dch --debian-branch=master --release --auto -N $(VERSION)
