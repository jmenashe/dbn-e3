# Authors: Majid Alkaee Taleghan, Mark Crowley, Thomas Dietterich
# Invasive Species Project
# 2012 Oregon State University
# Send code issues to: alkaee@gmail.com
# Date: 1/1/13:7:51 PM
#
# I used some of Brian Tanner's experiment code for invasive experiment.
#

import numpy
from numpy.numarray import array

import random

import math
import rlglue.RLGlue as RLGlue

from datetime import datetime
import os

REACHES=4
HABITATS=7

RESULTS_DIR = "invasive_results"

def demo():
    statistics = []
    this_score = evaluateAgent()
    printScore(0, this_score)
    statistics.append(this_score)

    for i in range(0, 10):
        for j in range(0, 10):
            RLGlue.RL_env_message("set-start-state " + startingState)
            RLGlue.RL_start()
            RLGlue.RL_episode(100)
        RLGlue.RL_env_message("set-start-state " + startingState)
        RLGlue.RL_start()
        this_score = evaluateAgent()
        printScore((i + 1) * 25, this_score)
        statistics.append(this_score)

    saveResultToCSV(
        statistics,
        REACHES,
        HABITATS,
        "results_%s.csv" % datetime.now().replace(microsecond=0).isoformat('_')
    )


def printScore(afterEpisodes, score_tuple):
    print "%d\t\t%.2f\t\t%.2f" % (afterEpisodes, score_tuple[0], score_tuple[1])

#
# Tell the agent to stop learning, then execute n episodes with his current
# policy. Estimate the mean and variance of the return over these episodes.
#
def evaluateAgent():
    sum = 0
    sum_of_squares = 0
    n = 10
    RLGlue.RL_agent_message("freeze learning")
    for i in range(0, n):
        RLGlue.RL_episode(100)
        this_return = RLGlue.RL_return()
        sum += this_return
        sum_of_squares += this_return ** 2

    mean = sum / n
    variance = (sum_of_squares - n * mean * mean) / (n - 1.0)
    standard_dev = math.sqrt(variance)

    RLGlue.RL_agent_message("unfreeze learning")
    return mean, standard_dev


def saveResultToCSV(statistics, reaches, habitats, fileName):
    filepath = os.path.join(RESULTS_DIR, fileName)
    if not os.path.exists(RESULTS_DIR):
        os.makedirs(RESULTS_DIR)

    numpy.savetxt(filepath, statistics, delimiter=",")

    with open(filepath, "a") as f:
        f.write("# Reaches: %s, Habitats: %s" % (reaches, habitats))


# Just do a single evaluateAgent and print it
def single_evaluation():
    this_score = evaluateAgent()
    printScore(0, this_score)

RLGlue.RL_init()
print "Telling the environment to use fixed start state."

rand = random.Random(1) # Use same state each time
startingState = array([rand.randint(1, 3) for i in xrange(REACHES * HABITATS)])

#S=array([1,1,2, 1, 3, 3, 1][0:nbrReaches * habitatSize])
startingState = ",".join(map(str, startingState))
print startingState

RLGlue.RL_env_message("set-start-state "+startingState)

RLGlue.RL_start()

print "Starting offline demo\n----------------------------\nWill alternate learning for 10 episodes, then freeze policy and evaluate for 10 episodes.\n"
print "After Episode\tMean Return\tStandard Deviation\n-------------------------------------------------------------------------"

demo()

print "Evaluating the agent again with the random start state:\n\t\tMean Return\tStandardDeviation\n-----------------------------------------------------"
RLGlue.RL_env_message("set-random-start-state")
single_evaluation()

RLGlue.RL_cleanup()
print "\nProgram Complete."