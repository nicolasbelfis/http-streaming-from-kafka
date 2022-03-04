import React, {useState} from "react";

export function CountTag() {
    const [count, setCount] = useState(new Map());
    const [connexion, setConnexion] = useState({});
    const [connected, setConnected] = useState(false);
    const [filterTags, setFilterTags] = useState("filters by tags...");


    function startStreaming() {
        let sse = new EventSource('http://localhost:8080/stream/countTags?filters=' + filterTags, {withCredentials: false})
        sse.onerror = () => {
            // error log here
            // after logging, close the connection
            setConnected(false)
            sse.close();
        }
        sse.onmessage = e => getRealtimeData(JSON.parse(e.data));
        setConnexion(sse)
        setConnected(true)
    }

    function stopStreaming() {
        setConnected(false)
        connexion.close()
    }

    function getRealtimeData(data) {
        setCount(prevState => {
            let newMap = new Map(prevState)
                newMap.set(data.first, data.second)
            return newMap
        })
    }

    function displayCount() {
        let divArray = []
        count.forEach((value, key) => divArray.push({value, key}))
        return divArray
    }

    return (<>
            <div className="col-sm-2">
                <p>tag count real time</p>
                <input className="small d-inline-block" value={filterTags}
                       onChange={evt => setFilterTags(evt.target.value)}/>
                <button className={connected ? "invisible" : "btn-primary"} onClick={() => startStreaming()}>start
                </button>
                <button className={connected ? "btn-primary" : "invisible"} onClick={() => stopStreaming()}>stop
                </button>
            </div>
            <div className="col-sm-10 border-1">
                {displayCount().map((value, index) => <div
                    key={value.key}><span className="fw-bold">{value.key}</span> counted <span className="fw-bold">{value.value}</span> times</div>)}
            </div>
        </>
    );
}