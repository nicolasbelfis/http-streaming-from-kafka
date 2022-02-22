import 'bootstrap/dist/css/bootstrap.min.css';

export function TweetList(props) {

    return (
        <div className="d-inline-block overflow-scroll me-2 bg-white ">
            <div className="small-height">
                {props.list.map((value, index) => (index < props.list.length - 1) ? <p key={parseInt(value.id)}>{value.text}</p> :
                    <p className="fw-bold" key={value.id}>{value.text}</p>)}
            </div>
        </div>
    );
}