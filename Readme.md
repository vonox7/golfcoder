# golfcoder.org

A code golf community leaderboard for [adventofcode.com](adventofcode.com), with a focus on code size.

Access the production site at [golfcoder.org](https://golfcoder.org).

Inspired by [SebLague's chess coding challenge](https://github.com/SebLague/Chess-Challenge) and it's community
leaderboard.

This project is not affiliated with adventofcode.com.

# Running the leaderboard locally

1. Install mongodb, at least version 4.0, see https://www.mongodb.com/docs/manual/administration/install-community/
2. Clone the repo, open it in IntelliJ, run the `Server` run configuration, and navigate to http://localhost:8030
3. Create a random user locally by clicking "Create random user (local only)" in the menu

# Deployment

The site is hosted on [Scalingo](https://scalingo.com), and is automatically deployed from the `main` branch.