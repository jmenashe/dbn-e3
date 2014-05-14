module Main where

import Data.List
import System.Environment

main = do
  [file] <- getArgs

  content <- readFile file

  let costs :: [Double]
      costs = map (read . takeWhile (/= ',')) (init $ lines content)
      table = zip [0, 10 ..] costs

  mapM_ (putStrLn . format) table
  
  where
    --format (i, cost) = unwords [show i, "&", show cost, "\\\\"]
    format (i, cost) = show cost
