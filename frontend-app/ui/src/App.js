import React, {useState} from 'react';
import * as PropTypes from "prop-types";
import {TweetList} from "./components/TweetList";

TweetList.propTypes = {list: PropTypes.arrayOf(PropTypes.any)};

export default function App() {

    const [tweets, setTweets] = useState([]);
    const [connexion, setConnexion] = useState({});


    function startStreaming() {
        let sse = new EventSource('http://localhost:8080/stream/sseTweets', {withCredentials: false})
        sse.onerror = () => {
            // error log here
            // after logging, close the connection
            sse.close();
        }
        sse.onmessage = e => getRealtimeData(JSON.parse(e.data));

        setConnexion(sse)
    }

    function stopStreaming() {
        connexion.close()
    }

    function getRealtimeData(data) {
        var newState = []
        setTweets(prevState => {
            prevState.forEach((value, index) => {
                if (prevState.length < 4) newState[index] = prevState[index]
                else if (index > 0 && value !== undefined && index < 4) newState[index - 1] = value
            })
            return [...newState, data]

        })
    }

    return (
        <div className="container">
            <div className="row border border-primary">
                <div className="col-sm">
                    <p>stream tweets real time</p>
                    <button onClick={() => startStreaming()}>start</button>
                    <button onClick={() => stopStreaming()}>stop</button>
                </div>
                <div className="col-sm border-1">
                    <TweetList list={tweets}/>
                </div>
            </div>
        </div>
    );
}
